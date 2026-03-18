package com.stablecoin.payments.onramp.infrastructure.provider.stripe;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.stablecoin.payments.onramp.domain.model.AccountType;
import com.stablecoin.payments.onramp.domain.model.BankAccount;
import com.stablecoin.payments.onramp.domain.model.Money;
import com.stablecoin.payments.onramp.domain.model.PaymentRail;
import com.stablecoin.payments.onramp.domain.model.PaymentRailType;
import com.stablecoin.payments.onramp.domain.port.PspGateway;
import com.stablecoin.payments.onramp.domain.port.PspPaymentRequest;
import com.stablecoin.payments.onramp.domain.port.PspPaymentResult;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = StripePspAdapterRetryTest.TestConfig.class)
@DisplayName("StripePspAdapter retry behavior")
class StripePspAdapterRetryTest {

    private static WireMockServer wireMock;
    private static final UUID COLLECTION_ID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

    @Autowired
    private PspGateway pspGateway;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("app.psp.provider", () -> "stripe");
        registry.add("resilience4j.retry.instances.stripe.max-attempts", () -> "3");
        registry.add("resilience4j.retry.instances.stripe.wait-duration", () -> "10ms");
        registry.add("resilience4j.retry.instances.stripe.retry-exceptions[0]",
                () -> "org.springframework.web.client.HttpServerErrorException");
        registry.add("resilience4j.retry.instances.stripe.ignore-exceptions[0]",
                () -> "org.springframework.web.client.HttpClientErrorException");
        registry.add("resilience4j.retry.retry-aspect-order", () -> "3");
        registry.add("resilience4j.circuitbreaker.circuit-breaker-aspect-order", () -> "1");
    }

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        circuitBreakerRegistry.getAllCircuitBreakers()
                .forEach(cb -> cb.transitionToClosedState());
    }

    private PspPaymentRequest aPaymentRequest() {
        return new PspPaymentRequest(
                COLLECTION_ID,
                new Money(new BigDecimal("250.00"), "USD"),
                new PaymentRail(PaymentRailType.ACH, "US", "USD"),
                new BankAccount("hash123", "021000021", AccountType.ACH_ROUTING, "US"),
                "stripe",
                COLLECTION_ID.toString()
        );
    }

    @Test
    @DisplayName("should retry on transient 503 failure then succeed")
    void shouldRetryOnTransientFailureThenSucceed() {
        wireMock.stubFor(post(urlEqualTo("/v1/payment_intents"))
                .inScenario("retry-then-success")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("second-attempt"));

        wireMock.stubFor(post(urlEqualTo("/v1/payment_intents"))
                .inScenario("retry-then-success")
                .whenScenarioStateIs("second-attempt")
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("third-attempt"));

        wireMock.stubFor(post(urlEqualTo("/v1/payment_intents"))
                .inScenario("retry-then-success")
                .whenScenarioStateIs("third-attempt")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": "pi_test123",
                                  "object": "payment_intent",
                                  "status": "succeeded",
                                  "amount": 25000,
                                  "amount_received": 25000,
                                  "currency": "usd",
                                  "client_secret": "pi_test123_secret_xxx",
                                  "capture_method": "automatic",
                                  "created": 1700000000
                                }
                                """)));

        var result = pspGateway.initiatePayment(aPaymentRequest());

        var expected = new PspPaymentResult("pi_test123", "succeeded");
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    @DisplayName("should not retry on 402 client error")
    void shouldNotRetryOnClientError() {
        wireMock.stubFor(post(urlEqualTo("/v1/payment_intents"))
                .willReturn(aResponse()
                        .withStatus(402)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "error": {
                                    "type": "card_error",
                                    "message": "Your card was declined."
                                  }
                                }
                                """)));

        assertThatThrownBy(() -> pspGateway.initiatePayment(aPaymentRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Stripe payment initiation unavailable");

        wireMock.verify(1, postRequestedFor(urlEqualTo("/v1/payment_intents")));
    }

    @Test
    @DisplayName("should exhaust retries on persistent 503 and invoke fallback")
    void shouldExhaustRetriesAndInvokeFallback() {
        wireMock.stubFor(post(urlEqualTo("/v1/payment_intents"))
                .willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> pspGateway.initiatePayment(aPaymentRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Stripe payment initiation unavailable");

        wireMock.verify(3, postRequestedFor(urlEqualTo("/v1/payment_intents")));
    }

    @Configuration
    @EnableAspectJAutoProxy
    @Import({RetryAutoConfiguration.class, CircuitBreakerAutoConfiguration.class})
    static class TestConfig {

        @Bean
        StripeProperties stripeProperties() {
            return new StripeProperties(wireMock.baseUrl(), "sk_test_xxx", 10);
        }

        @Bean
        StripePspAdapter stripePspAdapter(StripeProperties properties) {
            return new StripePspAdapter(properties);
        }
    }
}
