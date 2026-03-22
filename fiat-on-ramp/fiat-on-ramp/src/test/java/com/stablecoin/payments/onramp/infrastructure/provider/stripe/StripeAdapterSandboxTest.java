package com.stablecoin.payments.onramp.infrastructure.provider.stripe;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sandbox tests that run against the real Stripe test mode API.
 * <p>
 * Validates API key authentication and PaymentIntent creation without auto-confirm
 * (ACH auto-confirm requires a payment method which isn't available in headless tests).
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

    private RestClient restClient;

    @BeforeEach
    void setUp() {
        var apiKey = System.getenv("STRIPE_TEST_SECRET_KEY");
        var httpClient = HttpClient.newBuilder()
                .version(Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(15));

        restClient = RestClient.builder()
                .baseUrl("https://api.stripe.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .requestFactory(requestFactory)
                .build();
    }

    @Nested
    @DisplayName("PaymentIntent creation")
    class PaymentIntentCreation {

        @Test
        @DisplayName("should create a PaymentIntent in Stripe test mode without auto-confirm")
        void shouldCreatePaymentIntentInTestMode() {
            var formData = new LinkedMultiValueMap<String, String>();
            formData.add("amount", "5000");
            formData.add("currency", "usd");
            formData.add("payment_method_types[]", "us_bank_account");
            formData.add("metadata[sandbox_test]", "true");

            var response = restClient.post()
                    .uri("/v1/payment_intents")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(formData)
                    .retrieve()
                    .body(StripePaymentIntentResponse.class);

            assertThat(response).isNotNull();
            assertThat(response.id()).startsWith("pi_");
            assertThat(response.status()).isEqualTo("requires_payment_method");
        }

        @Test
        @DisplayName("should return same PaymentIntent for duplicate idempotency key")
        void shouldReturnSamePaymentIntentForDuplicateIdempotencyKey() {
            var idempotencyKey = UUID.randomUUID().toString();
            var formData = new LinkedMultiValueMap<String, String>();
            formData.add("amount", "7500");
            formData.add("currency", "usd");
            formData.add("payment_method_types[]", "us_bank_account");

            var first = restClient.post()
                    .uri("/v1/payment_intents")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .header("Idempotency-Key", idempotencyKey)
                    .body(formData)
                    .retrieve()
                    .body(StripePaymentIntentResponse.class);

            var second = restClient.post()
                    .uri("/v1/payment_intents")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .header("Idempotency-Key", idempotencyKey)
                    .body(formData)
                    .retrieve()
                    .body(StripePaymentIntentResponse.class);

            assertThat(second.id()).isEqualTo(first.id());
        }
    }
}
