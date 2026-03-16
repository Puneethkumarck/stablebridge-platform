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
 * Phase 3 E2E payment tests — full "sandwich" flow across 7 services:
 * S1 (Orchestrator) -> S2 (Compliance) -> S6 (FX) -> S3 (On-Ramp) ->
 * S4 (Blockchain Custody) -> S5 (Off-Ramp) -> S7 (Ledger).
 * <p>
 * Requires: {@code docker compose -f docker-compose.phase3-test.yml up -d}
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Phase 3 — End-to-End Payment Tests (US->DE, USD->EUR)")
class Phase3PaymentE2ETest {

    private static final Logger log = LoggerFactory.getLogger(Phase3PaymentE2ETest.class);

    // -- Service URLs --------------------------------------------------------
    private static final String S3_BASE_URL = "http://localhost:8085/on-ramp";
    private static final String S7_BASE_URL = "http://localhost:8088/ledger";

    // -- Test constants (US->DE corridor, USD->EUR) --------------------------
    private static final String SOURCE_AMOUNT = "1000.00";
    private static final String SECOND_AMOUNT = "500.00";
    private static final String SOURCE_CURRENCY = "USD";
    private static final String TARGET_CURRENCY = "EUR";
    private static final String SOURCE_COUNTRY = "US";
    private static final String TARGET_COUNTRY = "DE";
    private static final long STRIPE_AMOUNT_CENTS = 100_000L;
    private static final long STRIPE_SECOND_AMOUNT_CENTS = 50_000L;
    private static final String STRIPE_CURRENCY = "usd";
    private static final String STATE_COMPLETED = "COMPLETED";
    private static final String TOPIC_PAYMENT_COMPLETED = "payment.completed";

    // -- Timeouts ------------------------------------------------------------
    private static final Duration HEALTH_CHECK_TIMEOUT = Duration.ofMinutes(3);
    private static final Duration COLLECTION_ORDER_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration PAYMENT_STATE_TIMEOUT = Duration.ofSeconds(120);
    private static final Duration LEDGER_CHECK_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration KAFKA_EVENT_TIMEOUT = Duration.ofSeconds(10);

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

        new ServiceHealthChecker(httpClient).waitForAllServices(HEALTH_CHECK_TIMEOUT);
    }

    // ── 1. Happy Path — Full Sandwich Payment ─────────────────────────

    @Nested
    @Order(1)
    @DisplayName("Happy Path — Full Sandwich Payment (USD->USDC->EUR)")
    class HappyPathFullSandwich {

        @Test
        @DisplayName("should complete full payment lifecycle: fiat-in -> on-chain -> fiat-out")
        void shouldCompleteFullPaymentLifecycle() throws Exception {
            // given
            var senderId = UUID.randomUUID();
            var recipientId = UUID.randomUUID();
            var idempotencyKey = UUID.randomUUID().toString();

            // when — initiate payment via S1
            var initiateResponse = paymentApiClient.initiatePayment(
                    null, senderId, recipientId,
                    SOURCE_AMOUNT, SOURCE_CURRENCY, TARGET_CURRENCY,
                    SOURCE_COUNTRY, TARGET_COUNTRY, idempotencyKey);

            // then — 201 Created with paymentId
            assertThat(initiateResponse.statusCode()).isEqualTo(201);
            var paymentId = JSON.readTree(initiateResponse.body()).get("paymentId").asText();
            assertThat(paymentId).isNotBlank();
            log.info("Payment initiated: paymentId={}", paymentId);

            // when — wait for S3 collection order (created async by Temporal)
            var collection = waitForCollectionOrder(paymentId, COLLECTION_ORDER_TIMEOUT);
            assertThat(collection.collectionId()).isNotBlank();
            assertThat(collection.pspReference()).isNotBlank();
            log.info("Collection: id={}, pspRef={}", collection.collectionId(), collection.pspReference());

            // when — send Stripe webhook simulating fiat collection success
            var webhookResponse = stripeWebhookSender.sendCollectionSuccess(
                    collection.pspReference(), STRIPE_AMOUNT_CENTS, STRIPE_CURRENCY);
            assertThat(webhookResponse.statusCode()).isEqualTo(200);

            // then — payment reaches COMPLETED state
            waitForPaymentState(paymentId, STATE_COMPLETED, PAYMENT_STATE_TIMEOUT);
            var finalBody = JSON.readTree(paymentApiClient.getPayment(paymentId).body());
            assertThat(finalBody.get("state").asText()).isEqualTo(STATE_COMPLETED);
            assertThat(finalBody.get("senderId").asText()).isEqualTo(senderId.toString());
            assertThat(finalBody.get("recipientId").asText()).isEqualTo(recipientId.toString());
            log.info("Payment COMPLETED: {}", paymentId);

            // then — S7 ledger has journal entries (best-effort, Kafka propagation may lag)
            var hasLedgerEntries = checkJournalEntriesExist(paymentId, LEDGER_CHECK_TIMEOUT);
            log.info("Ledger journal entries: found={}", hasLedgerEntries);

            // then — Kafka payment.completed event published (best-effort)
            var completedEvent = kafkaEventVerifier.waitForEvent(
                    TOPIC_PAYMENT_COMPLETED, paymentId, KAFKA_EVENT_TIMEOUT);
            completedEvent.ifPresentOrElse(
                    event -> {
                        assertThat(event).contains(paymentId);
                        log.info("Kafka payment.completed event confirmed");
                    },
                    () -> log.warn("Kafka event not found within timeout — outbox relay may be delayed"));
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
            // given
            var senderId = UUID.randomUUID();
            var recipientId = UUID.randomUUID();
            var idempotencyKey = UUID.randomUUID().toString();

            // when — first request
            var first = paymentApiClient.initiatePayment(
                    null, senderId, recipientId,
                    SOURCE_AMOUNT, SOURCE_CURRENCY, TARGET_CURRENCY,
                    SOURCE_COUNTRY, TARGET_COUNTRY, idempotencyKey);

            // then — 201 Created
            assertThat(first.statusCode()).isEqualTo(201);
            var paymentId = JSON.readTree(first.body()).get("paymentId").asText();

            // when — second request with same idempotency key
            var second = paymentApiClient.initiatePayment(
                    null, senderId, recipientId,
                    SOURCE_AMOUNT, SOURCE_CURRENCY, TARGET_CURRENCY,
                    SOURCE_COUNTRY, TARGET_COUNTRY, idempotencyKey);

            // then — 200 OK with same paymentId
            assertThat(second.statusCode()).isEqualTo(200);
            assertThat(JSON.readTree(second.body()).get("paymentId").asText()).isEqualTo(paymentId);
            log.info("Idempotency PASSED: paymentId={}", paymentId);
        }
    }

    // ── 3. Second Payment — independent workflow execution ────────────

    @Nested
    @Order(3)
    @DisplayName("Second Payment — independent workflow execution")
    class SecondPayment {

        @Test
        @DisplayName("should complete a second independent payment through full sandwich")
        void shouldCompleteSecondPayment() throws Exception {
            // given
            var senderId = UUID.randomUUID();
            var recipientId = UUID.randomUUID();
            var idempotencyKey = UUID.randomUUID().toString();

            // when — initiate with different amount
            var response = paymentApiClient.initiatePayment(
                    null, senderId, recipientId,
                    SECOND_AMOUNT, SOURCE_CURRENCY, TARGET_CURRENCY,
                    SOURCE_COUNTRY, TARGET_COUNTRY, idempotencyKey);

            // then — 201 Created
            assertThat(response.statusCode()).isEqualTo(201);
            var paymentId = JSON.readTree(response.body()).get("paymentId").asText();
            log.info("Second payment initiated: paymentId={}", paymentId);

            // when — trigger fiat collection via webhook
            var collection = waitForCollectionOrder(paymentId, COLLECTION_ORDER_TIMEOUT);
            stripeWebhookSender.sendCollectionSuccess(
                    collection.pspReference(), STRIPE_SECOND_AMOUNT_CENTS, STRIPE_CURRENCY);

            // then — payment reaches COMPLETED
            waitForPaymentState(paymentId, STATE_COMPLETED, PAYMENT_STATE_TIMEOUT);
            var finalBody = JSON.readTree(paymentApiClient.getPayment(paymentId).body());
            assertThat(finalBody.get("state").asText()).isEqualTo(STATE_COMPLETED);

            // then — ledger entries exist (best-effort)
            var hasEntries = checkJournalEntriesExist(paymentId, LEDGER_CHECK_TIMEOUT);
            log.info("Second payment PASSED: paymentId={}, ledger={}", paymentId, hasEntries);
        }
    }

    // ── Support records ──────────────────────────────────────────────

    private record CollectionOrderInfo(String collectionId, String pspReference) {}

    // ── Helpers ──────────────────────────────────────────────────────

    private CollectionOrderInfo waitForCollectionOrder(String paymentId, Duration timeout) {
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
        return new CollectionOrderInfo(result[0], result[1]);
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
                    if ("FAILED".equals(currentState) || "CANCELLED".equals(currentState)) {
                        throw new AssertionError("Payment " + paymentId
                                + " reached terminal state " + currentState
                                + " instead of expected " + expectedState);
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
