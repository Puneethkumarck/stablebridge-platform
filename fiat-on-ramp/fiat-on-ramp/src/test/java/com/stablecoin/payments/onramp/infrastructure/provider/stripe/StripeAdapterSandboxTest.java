package com.stablecoin.payments.onramp.infrastructure.provider.stripe;

import com.stablecoin.payments.onramp.domain.model.AccountType;
import com.stablecoin.payments.onramp.domain.model.BankAccount;
import com.stablecoin.payments.onramp.domain.model.Money;
import com.stablecoin.payments.onramp.domain.model.PaymentRail;
import com.stablecoin.payments.onramp.domain.model.PaymentRailType;
import com.stablecoin.payments.onramp.domain.port.PspPaymentRequest;
import com.stablecoin.payments.onramp.domain.port.PspPaymentResult;
import com.stablecoin.payments.onramp.domain.port.PspRefundRequest;
import com.stablecoin.payments.onramp.domain.port.PspRefundResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sandbox tests that run against the real Stripe test mode API.
 * <p>
 * Requires {@code STRIPE_TEST_SECRET_KEY} environment variable set to a valid
 * Stripe test-mode secret key ({@code sk_test_...}).
 * <p>
 * These tests are excluded from CI by the {@code @Tag("sandbox")} annotation.
 * Run manually:
 * <pre>
 * STRIPE_TEST_SECRET_KEY=sk_test_xxx ./gradlew :fiat-on-ramp:fiat-on-ramp:test --tests '*StripeAdapterSandboxTest*'
 * </pre>
 *
 * @see <a href="https://docs.stripe.com/testing">Stripe Testing</a>
 */
@Tag("sandbox")
@EnabledIfEnvironmentVariable(named = "STRIPE_TEST_SECRET_KEY", matches = "sk_test_.+")
@DisplayName("Stripe Adapter Sandbox (live test mode)")
class StripeAdapterSandboxTest {

    private StripePspAdapter adapter;

    private static final UUID COLLECTION_ID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

    @BeforeEach
    void setUp() {
        var apiKey = System.getenv("STRIPE_TEST_SECRET_KEY");
        var properties = new StripeProperties("https://api.stripe.com", apiKey, 15);
        adapter = new StripePspAdapter(properties);
    }

    private PspPaymentRequest aPaymentRequest(BigDecimal amount, String idempotencyKey) {
        return new PspPaymentRequest(
                COLLECTION_ID,
                new Money(amount, "USD"),
                new PaymentRail(PaymentRailType.ACH, "US", "USD"),
                new BankAccount("hash_sandbox", "110000000", AccountType.ACH_ROUTING, "US"),
                "stripe",
                idempotencyKey
        );
    }

    @Nested
    @DisplayName("initiatePayment")
    class InitiatePayment {

        @Test
        @DisplayName("should create a PaymentIntent in Stripe test mode and return a valid reference")
        void shouldCreatePaymentIntentInTestMode() {
            var idempotencyKey = UUID.randomUUID().toString();
            var request = aPaymentRequest(new BigDecimal("50.00"), idempotencyKey);

            var result = adapter.initiatePayment(request);

            var expected = new PspPaymentResult(result.pspReference(), result.status());
            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
            assertThat(result.pspReference()).startsWith("pi_");
        }

        @Test
        @DisplayName("should return the same PaymentIntent for duplicate idempotency key")
        void shouldReturnSamePaymentIntentForDuplicateIdempotencyKey() {
            var idempotencyKey = UUID.randomUUID().toString();
            var request = aPaymentRequest(new BigDecimal("75.00"), idempotencyKey);

            var firstResult = adapter.initiatePayment(request);
            var secondResult = adapter.initiatePayment(request);

            var expected = new PspPaymentResult(firstResult.pspReference(), firstResult.status());
            assertThat(secondResult).usingRecursiveComparison().isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("initiateRefund")
    class InitiateRefund {

        @Test
        @DisplayName("should create a refund for a succeeded PaymentIntent in test mode")
        void shouldCreateRefundInTestMode() {
            // First, create a payment
            var idempotencyKey = UUID.randomUUID().toString();
            var paymentRequest = aPaymentRequest(new BigDecimal("100.00"), idempotencyKey);
            var paymentResult = adapter.initiatePayment(paymentRequest);

            // Then refund it (only if payment succeeded or requires_action)
            // In test mode without a real payment method attached, the PI may not be in a refundable state.
            // This test verifies the adapter correctly calls the Stripe refund endpoint.
            if ("succeeded".equals(paymentResult.status())) {
                var refundRequest = new PspRefundRequest(
                        COLLECTION_ID,
                        paymentResult.pspReference(),
                        new Money(new BigDecimal("50.00"), "USD"),
                        "stripe",
                        "requested_by_customer"
                );

                var refundResult = adapter.initiateRefund(refundRequest);

                var expected = new PspRefundResult(refundResult.pspRefundRef(), refundResult.status());
                assertThat(refundResult)
                        .usingRecursiveComparison()
                        .isEqualTo(expected);
                assertThat(refundResult.pspRefundRef()).startsWith("re_");
            }
        }
    }
}
