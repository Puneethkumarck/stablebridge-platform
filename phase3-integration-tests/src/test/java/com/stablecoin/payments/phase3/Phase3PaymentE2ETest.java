package com.stablecoin.payments.phase3;

import com.stablecoin.payments.phase3.support.KafkaEventVerifier;
import com.stablecoin.payments.phase3.support.ModulrWebhookSender;
import com.stablecoin.payments.phase3.support.PaymentApiClient;
import com.stablecoin.payments.phase3.support.ServiceHealthChecker;
import com.stablecoin.payments.phase3.support.StripeWebhookSender;
import com.stablecoin.payments.phase3.support.WireMockAdminClient;
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

    private static final String S7_BASE_URL = "http://localhost:8088/ledger";

    private static final JsonMapper JSON = JsonMapper.builder()
            .findAndAddModules()
            .build();

    private HttpClient httpClient;
    private PaymentApiClient paymentApiClient;
    private KafkaEventVerifier kafkaEventVerifier;
    private WireMockAdminClient wireMockAdminClient;
    private StripeWebhookSender stripeWebhookSender;
    private ModulrWebhookSender modulrWebhookSender;

    @BeforeAll
    void setupAll() {
        httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        paymentApiClient = new PaymentApiClient(httpClient);
        kafkaEventVerifier = new KafkaEventVerifier();
        wireMockAdminClient = new WireMockAdminClient(httpClient);
        stripeWebhookSender = new StripeWebhookSender(httpClient);
        modulrWebhookSender = new ModulrWebhookSender(httpClient);

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
            log.info("Payment initiated: paymentId={}", paymentId);

            // 2. Wait for workflow to reach fiat collection (S3 creates collection order)
            //    The S1 REST API state stays INITIATED while Temporal runs the workflow.
            //    Poll S3 directly for the collection order created by the workflow.
            log.info("Step 2: Waiting for S3 collection order to be created");
            var collectionInfo = waitForCollectionOrderInfo(paymentId, Duration.ofSeconds(30));
            var collectionId = collectionInfo[0];
            var pspReference = collectionInfo[1];
            log.info("Collection order created: collectionId={}, pspReference={}", collectionId, pspReference);

            // 3. Send Stripe webhook to S3 simulating fiat collection success
            //    Use the pspReference (pi_xxx) as the charge ID — S3 looks up by pspReference
            log.info("Step 3: Sending Stripe collection success webhook");
            stripeWebhookSender.sendCollectionSuccess(pspReference, 100000L, "usd");
            log.info("Stripe webhook sent for collectionId={}", collectionId);

            // 4. Wait for payment to reach terminal state (COMPLETED or FAILED)
            //    The workflow: fiat signal → chain transfer → chain confirm signal → off-ramp → COMPLETED
            //    DevCustodyAdapter auto-confirms, so chain signal should arrive automatically.
            //    Off-ramp with fallback adapters completes immediately.
            log.info("Step 4: Waiting for terminal state (COMPLETED or FAILED)");
            var finalState = waitForPaymentStateOneOf(paymentId,
                    new String[]{"COMPLETED", "FAILED"},
                    Duration.ofSeconds(120));

            // 5. Verify final state
            var finalResponse = paymentApiClient.getPayment(paymentId);
            var finalBody = JSON.readTree(finalResponse.body());
            log.info("Final payment state: {}", finalBody.get("state").asText());

            assertThat(finalBody.get("state").asText()).isEqualTo("COMPLETED");

            log.info("Happy path test PASSED — full sandwich payment completed");
        }
    }

    // ── 2. Compliance Rejection ───────────────────────────────────────

    @Nested
    @Order(2)
    @DisplayName("Second Payment — concurrent workflow execution")
    class SecondPayment {

        @Test
        @DisplayName("should complete a second payment while first has already finished")
        void shouldCompleteSecondPayment() throws Exception {
            var senderId = UUID.randomUUID();
            var recipientId = UUID.randomUUID();
            var idempotencyKey = UUID.randomUUID().toString();

            // Initiate a second payment
            log.info("Initiating second payment");
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
            log.info("Collection order: id={}, pspRef={}", collectionInfo[0], collectionInfo[1]);
            stripeWebhookSender.sendCollectionSuccess(collectionInfo[1], 50000L, "usd");

            // Wait for terminal state
            log.info("Waiting for terminal state");
            waitForPaymentStateOneOf(paymentId,
                    new String[]{"COMPLETED", "FAILED"},
                    Duration.ofSeconds(120));

            var finalResponse = paymentApiClient.getPayment(paymentId);
            var finalBody = JSON.readTree(finalResponse.body());
            var state = finalBody.get("state").asText();
            log.info("Second payment state: {}", state);

            assertThat(state).isEqualTo("COMPLETED");
            log.info("Second payment test PASSED");
        }
    }

    // ── 3. Idempotency ────────────────────────────────────────────────

    @Nested
    @Order(3)
    @DisplayName("Idempotency — duplicate key returns existing payment")
    class Idempotency {

        @Test
        @DisplayName("should return 200 OK with same paymentId on duplicate idempotency key")
        void shouldReturnExistingPaymentOnDuplicateKey() throws Exception {
            var senderId = UUID.randomUUID();
            var recipientId = UUID.randomUUID();
            var idempotencyKey = UUID.randomUUID().toString();

            // 1. First request → 201 Created
            log.info("First payment request with idempotency key: {}", idempotencyKey);
            var first = paymentApiClient.initiatePayment(
                    null, senderId, recipientId,
                    "1000.00", "USD", "EUR",
                    "US", "DE", idempotencyKey);
            assertThat(first.statusCode()).isEqualTo(201);

            var firstBody = JSON.readTree(first.body());
            var paymentId = firstBody.get("paymentId").asText();
            log.info("First request: paymentId={}, status={}", paymentId, first.statusCode());

            // 2. Second request with same idempotency key → 200 OK
            log.info("Second payment request with same idempotency key");
            var second = paymentApiClient.initiatePayment(
                    null, senderId, recipientId,
                    "1000.00", "USD", "EUR",
                    "US", "DE", idempotencyKey);
            assertThat(second.statusCode()).isEqualTo(200);

            var secondBody = JSON.readTree(second.body());
            var secondPaymentId = secondBody.get("paymentId").asText();
            log.info("Second request: paymentId={}, status={}", secondPaymentId, second.statusCode());

            // 3. Both return same paymentId
            assertThat(secondPaymentId).isEqualTo(paymentId);

            log.info("Idempotency test PASSED — same paymentId returned for duplicate key");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private static final String S3_BASE_URL = "http://localhost:8085/on-ramp";

    /**
     * Polls S3 directly until a collection order exists for the given paymentId.
     * Returns [collectionId, pspReference]. The workflow creates the collection order async via Temporal.
     */
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
                            log.info("Found collection order for payment {}: id={}, pspRef={}",
                                    paymentId, result[0], result[1]);
                            return true;
                        }
                    }
                    return false;
                });
        return result;
    }

    private String waitForPaymentState(String paymentId, String expectedState,
                                        Duration timeout) {
        return waitForPaymentStateOneOf(paymentId, new String[]{expectedState}, timeout);
    }

    private String waitForPaymentStateOneOf(String paymentId, String[] expectedStates,
                                             Duration timeout) {
        var result = new String[1];
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

                    // Check if current state matches any expected state
                    for (String expected : expectedStates) {
                        if (expected.equals(currentState)) {
                            result[0] = currentState;
                            return true;
                        }
                    }

                    // Fail fast if payment has reached a terminal failure state
                    if ("FAILED".equals(currentState) || "CANCELLED".equals(currentState)) {
                        var isExpected = false;
                        for (String expected : expectedStates) {
                            if ("FAILED".equals(expected) || "CANCELLED".equals(expected)
                                    || "COMPLIANCE_REJECTED".equals(expected)) {
                                isExpected = true;
                                break;
                            }
                        }
                        if (!isExpected) {
                            throw new AssertionError("Payment " + paymentId
                                    + " reached terminal state " + currentState
                                    + " instead of expected " + String.join("/", expectedStates));
                        }
                        result[0] = currentState;
                        return true;
                    }

                    return false;
                });
        return result[0];
    }

    private String extractFieldFromPayment(String paymentId, String fieldName) {
        try {
            var response = paymentApiClient.getPayment(paymentId);
            if (response.statusCode() == 200) {
                var body = JSON.readTree(response.body());
                if (body.has(fieldName) && !body.get(fieldName).isNull()) {
                    return body.get(fieldName).asText();
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract field {} from payment {}: {}", fieldName, paymentId, e.getMessage());
        }
        return null;
    }

    private void verifyJournalEntriesExist(String paymentId) {
        await().atMost(Duration.ofSeconds(30))
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
                    var hasEntries = body.isArray() ? body.size() > 0
                            : (body.has("content") && body.get("content").size() > 0);
                    log.info("S7 journal entries for payment {}: found={}", paymentId, hasEntries);
                    return hasEntries;
                });
    }

}
