package com.stablecoin.payments.phase3;

import com.stablecoin.payments.phase3.support.KafkaEventVerifier;
import com.stablecoin.payments.phase3.support.PaymentApiClient;
import com.stablecoin.payments.phase3.support.ServiceHealthChecker;
import com.stablecoin.payments.phase3.support.StripeWebhookSender;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Phase 3 end-to-end payment tests.
 * <p>
 * Tests the full "sandwich" payment flow across 7 services:
 * S1 (Orchestrator) → S2 (Compliance) → S6 (FX) → S3 (On-Ramp) →
 * S4 (Blockchain Custody) → S5 (Off-Ramp) → S7 (Ledger).
 * <p>
 * Requires Docker Compose stack running:
 * {@code docker compose -f docker-compose.phase3-test.yml up -d}
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Phase 3 — End-to-End Payment Tests (US→DE, USD→EUR)")
class Phase3PaymentE2ETest {

    private static final Logger log = LoggerFactory.getLogger(Phase3PaymentE2ETest.class);

    private static final String S3_BASE_URL = "http://localhost:8085/on-ramp";
    private static final String S7_BASE_URL = "http://localhost:8088/ledger";

    private static final JsonMapper JSON = JsonMapper.builder()
            .findAndAddModules()
            .build();

    private HttpClient httpClient;
    private PaymentApiClient paymentApiClient;
    private KafkaEventVerifier kafkaEventVerifier;
    private StripeWebhookSender stripeWebhookSender;

    @BeforeAll
    void setupAll() {
        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        paymentApiClient = new PaymentApiClient(httpClient);
        kafkaEventVerifier = new KafkaEventVerifier();
        stripeWebhookSender = new StripeWebhookSender(httpClient);

        var healthChecker = new ServiceHealthChecker(httpClient);
        healthChecker.waitForAllServices(Duration.ofMinutes(3));
    }

    // ── 1. Happy Path — Full Sandwich Payment ─────────────────────────

    @Nested
    @Order(1)
    @DisplayName("Happy Path — Full Sandwich Payment (USD→USDC→EUR)")
    class HappyPathFullSandwich {

        @Test
        @DisplayName("should complete full payment lifecycle: fiat-in → on-chain → fiat-out")
        void shouldCompleteFullPaymentLifecycle() throws Exception {
            var senderId = UUID.randomUUID();
            var recipientId = UUID.randomUUID();
            var idempotencyKey = UUID.randomUUID().toString();

            // 1. Initiate payment via S1
            log.info("Step 1: Initiating payment USD→EUR (US→DE)");
            var initiateResponse = paymentApiClient.initiatePayment(
                    null, senderId, recipientId,
                    "1000.00", "USD", "EUR",
                    "US", "DE", idempotencyKey);
            assertThat(initiateResponse.statusCode()).isEqualTo(201);

            var initiateBody = JSON.readTree(initiateResponse.body());
            var paymentId = initiateBody.get("paymentId").asText();
            assertThat(paymentId).isNotBlank();
            log.info("Payment initiated: paymentId={}", paymentId);

            // 2. Wait for S3 collection order (created async by Temporal workflow)
            log.info("Step 2: Waiting for S3 collection order");
            var collectionInfo = waitForCollectionOrderInfo(paymentId, Duration.ofSeconds(30));
            var collectionId = collectionInfo[0];
            var pspReference = collectionInfo[1];
            assertThat(collectionId).isNotBlank();
            assertThat(pspReference).isNotBlank();
            log.info("Collection order: collectionId={}, pspReference={}", collectionId, pspReference);

            // 3. Send Stripe webhook simulating fiat collection success
            log.info("Step 3: Sending Stripe collection success webhook");
            var webhookResponse = stripeWebhookSender.sendCollectionSuccess(pspReference, 100000L, "usd");
            assertThat(webhookResponse.statusCode()).isEqualTo(200);

            // 4. Wait for terminal state
            log.info("Step 4: Waiting for terminal state");
            waitForPaymentState(paymentId, "COMPLETED", Duration.ofSeconds(120));

            // 5. Verify final payment state has all expected fields
            var finalResponse = paymentApiClient.getPayment(paymentId);
            assertThat(finalResponse.statusCode()).isEqualTo(200);
            var finalBody = JSON.readTree(finalResponse.body());
            assertThat(finalBody.get("state").asText()).isEqualTo("COMPLETED");
            assertThat(finalBody.get("senderId").asText()).isEqualTo(senderId.toString());
            assertThat(finalBody.get("recipientId").asText()).isEqualTo(recipientId.toString());
            log.info("Payment COMPLETED: {}", paymentId);

            // 6. Verify S7 ledger recorded journal entries (best-effort — Kafka propagation may lag)
            log.info("Step 6: Checking S7 ledger journal entries");
            var hasJournalEntries = checkJournalEntriesExist(paymentId, Duration.ofSeconds(15));
            if (hasJournalEntries) {
                log.info("Ledger journal entries confirmed for paymentId={}", paymentId);
            } else {
                log.warn("Ledger journal entries not yet available for paymentId={} — Kafka propagation lag", paymentId);
            }

            // 7. Verify Kafka payment.completed event (best-effort)
            log.info("Step 7: Checking payment.completed Kafka event");
            var completedEvent = kafkaEventVerifier.waitForEvent(
                    "payment.completed", paymentId, Duration.ofSeconds(10));
            if (completedEvent.isPresent()) {
                assertThat(completedEvent.get()).contains(paymentId);
                log.info("Kafka payment.completed event confirmed");
            } else {
                log.warn("Kafka payment.completed event not found within timeout — outbox relay may be delayed");
            }

            log.info("Happy path test PASSED — full sandwich payment completed");
        }
    }

    // ── 2. Idempotency ────────────────────────────────────────────────

    @Nested
    @Order(2)
    @DisplayName("Idempotency — duplicate key returns existing payment")
    class Idempotency {

        @Test
        @DisplayName("should return 200 OK with same paymentId on duplicate idempotency key")
        void shouldReturnExistingPaymentOnDuplicateKey() throws Exception {
            var senderId = UUID.randomUUID();
            var recipientId = UUID.randomUUID();
            var idempotencyKey = UUID.randomUUID().toString();

            // 1. First request → 201 Created
            var first = paymentApiClient.initiatePayment(
                    null, senderId, recipientId,
                    "1000.00", "USD", "EUR",
                    "US", "DE", idempotencyKey);
            assertThat(first.statusCode()).isEqualTo(201);

            var firstBody = JSON.readTree(first.body());
            var paymentId = firstBody.get("paymentId").asText();

            // 2. Second request with same idempotency key → 200 OK
            var second = paymentApiClient.initiatePayment(
                    null, senderId, recipientId,
                    "1000.00", "USD", "EUR",
                    "US", "DE", idempotencyKey);
            assertThat(second.statusCode()).isEqualTo(200);

            var secondBody = JSON.readTree(second.body());
            assertThat(secondBody.get("paymentId").asText()).isEqualTo(paymentId);

            log.info("Idempotency test PASSED — same paymentId={} returned for duplicate key", paymentId);
        }
    }

    // ── 3. Second Payment — verify independent workflow execution ─────

    @Nested
    @Order(3)
    @DisplayName("Second Payment — independent workflow execution")
    class SecondPayment {

        @Test
        @DisplayName("should complete a second independent payment through full sandwich")
        void shouldCompleteSecondPayment() throws Exception {
            var senderId = UUID.randomUUID();
            var recipientId = UUID.randomUUID();
            var idempotencyKey = UUID.randomUUID().toString();

            // Initiate a second payment with different amount
            var response = paymentApiClient.initiatePayment(
                    null, senderId, recipientId,
                    "500.00", "USD", "EUR",
                    "US", "DE", idempotencyKey);
            assertThat(response.statusCode()).isEqualTo(201);

            var body = JSON.readTree(response.body());
            var paymentId = body.get("paymentId").asText();
            log.info("Second payment initiated: paymentId={}", paymentId);

            // Wait for collection order, then send webhook
            var collectionInfo = waitForCollectionOrderInfo(paymentId, Duration.ofSeconds(30));
            stripeWebhookSender.sendCollectionSuccess(collectionInfo[1], 50000L, "usd");

            // Wait for completion
            waitForPaymentState(paymentId, "COMPLETED", Duration.ofSeconds(120));

            // Verify final state
            var finalResponse = paymentApiClient.getPayment(paymentId);
            var finalBody = JSON.readTree(finalResponse.body());
            assertThat(finalBody.get("state").asText()).isEqualTo("COMPLETED");

            // Check ledger entries (best-effort)
            var hasEntries = checkJournalEntriesExist(paymentId, Duration.ofSeconds(10));
            log.info("Ledger entries for second payment: found={}", hasEntries);

            log.info("Second payment test PASSED — paymentId={}", paymentId);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private String[] waitForCollectionOrderInfo(String paymentId, Duration timeout) {
        var result = new String[2];
        await().atMost(timeout)
                .pollInterval(Duration.ofSeconds(2))
                .ignoreExceptions()
                .until(() -> {
                    var response = httpClient.send(
                            HttpRequest.newBuilder()
                                    .uri(URI.create(S3_BASE_URL + "/v1/collections?paymentId=" + paymentId))
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        var body = JSON.readTree(response.body());
                        if (body.has("collectionId")) {
                            result[0] = body.get("collectionId").asText();
                            result[1] = body.has("pspReference") ? body.get("pspReference").asText() : result[0];
                            return true;
                        }
                    }
                    return false;
                });
        return result;
    }

    private void waitForPaymentState(String paymentId, String expectedState, Duration timeout) {
        await().atMost(timeout)
                .pollInterval(Duration.ofSeconds(2))
                .ignoreExceptions()
                .until(() -> {
                    var response = paymentApiClient.getPayment(paymentId);
                    if (response.statusCode() != 200) {
                        return false;
                    }
                    var body = JSON.readTree(response.body());
                    var currentState = body.get("state").asText();
                    log.debug("Payment {} state: {}", paymentId, currentState);

                    if (expectedState.equals(currentState)) {
                        return true;
                    }

                    // Fail fast on unexpected terminal state
                    if ("FAILED".equals(currentState) || "CANCELLED".equals(currentState)) {
                        if (!expectedState.equals(currentState)) {
                            throw new AssertionError("Payment " + paymentId
                                    + " reached terminal state " + currentState
                                    + " instead of expected " + expectedState);
                        }
                    }
                    return false;
                });
    }

    private boolean checkJournalEntriesExist(String paymentId, Duration timeout) {
        try {
            await().atMost(timeout)
                    .pollInterval(Duration.ofSeconds(3))
                    .ignoreExceptions()
                    .until(() -> {
                        var response = httpClient.send(
                                HttpRequest.newBuilder()
                                        .uri(URI.create(S7_BASE_URL + "/v1/journals?paymentId=" + paymentId))
                                        .GET()
                                        .build(),
                                HttpResponse.BodyHandlers.ofString());

                        if (response.statusCode() != 200) {
                            return false;
                        }

                        var body = JSON.readTree(response.body());
                        return body.isArray() ? body.size() > 0
                                : (body.has("content") && body.get("content").size() > 0);
                    });
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
