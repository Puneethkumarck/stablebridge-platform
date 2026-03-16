package com.stablecoin.payments.offramp.infrastructure.provider.circle;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.stablecoin.payments.offramp.domain.port.RedemptionGateway;
import com.stablecoin.payments.offramp.domain.port.RedemptionRequest;
import com.stablecoin.payments.offramp.domain.port.RedemptionResult;
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
import java.time.Instant;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = CircleRedemptionAdapterRetryTest.TestConfig.class)
@DisplayName("CircleRedemptionAdapter retry behavior")
class CircleRedemptionAdapterRetryTest {

    private static WireMockServer wireMock;
    private static final UUID PAYOUT_ID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

    @Autowired
    private RedemptionGateway redemptionGateway;

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
        registry.add("app.redemption.provider", () -> "circle");
        registry.add("resilience4j.retry.instances.circle.max-attempts", () -> "3");
        registry.add("resilience4j.retry.instances.circle.wait-duration", () -> "10ms");
        registry.add("resilience4j.retry.instances.circle.retry-exceptions[0]",
                () -> "org.springframework.web.client.HttpServerErrorException");
        registry.add("resilience4j.retry.instances.circle.ignore-exceptions[0]",
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

    private RedemptionRequest aRedemptionRequest() {
        return new RedemptionRequest(PAYOUT_ID, "USDC", new BigDecimal("10000.000000"), BigDecimal.ONE);
    }

    @Test
    @DisplayName("should retry on transient 503 failure then succeed")
    void shouldRetryOnTransientFailureThenSucceed() {
        wireMock.stubFor(post(urlEqualTo("/v1/businessAccount/payouts"))
                .inScenario("retry-then-success")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("second-attempt"));

        wireMock.stubFor(post(urlEqualTo("/v1/businessAccount/payouts"))
                .inScenario("retry-then-success")
                .whenScenarioStateIs("second-attempt")
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "data": {
                                    "id": "circle-payout-ref-001",
                                    "amount": {
                                      "amount": "10000.00",
                                      "currency": "USD"
                                    },
                                    "status": "pending",
                                    "createDate": "2026-03-10T12:00:00.000Z"
                                  }
                                }
                                """)));

        var result = redemptionGateway.redeem(aRedemptionRequest());

        var expected = new RedemptionResult(
                "circle-payout-ref-001",
                new BigDecimal("10000.00"),
                "USD",
                Instant.parse("2026-03-10T12:00:00.000Z")
        );
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    @DisplayName("should not retry on 400 client error")
    void shouldNotRetryOnClientError() {
        wireMock.stubFor(post(urlEqualTo("/v1/businessAccount/payouts"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "code": 2,
                                  "message": "Invalid idempotency key"
                                }
                                """)));

        assertThatThrownBy(() -> redemptionGateway.redeem(aRedemptionRequest()))
                .isInstanceOf(Exception.class);

        wireMock.verify(1, postRequestedFor(urlEqualTo("/v1/businessAccount/payouts")));
    }

    @Test
    @DisplayName("should exhaust retries on persistent 503 and invoke fallback")
    void shouldExhaustRetriesAndInvokeFallback() {
        wireMock.stubFor(post(urlEqualTo("/v1/businessAccount/payouts"))
                .willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> redemptionGateway.redeem(aRedemptionRequest()))
                .isInstanceOf(Exception.class);

        wireMock.verify(3, postRequestedFor(urlEqualTo("/v1/businessAccount/payouts")));
    }

    @Configuration
    @EnableAspectJAutoProxy
    @Import({RetryAutoConfiguration.class, CircuitBreakerAutoConfiguration.class})
    static class TestConfig {

        @Bean
        CircleProperties circleProperties() {
            return new CircleProperties(
                    wireMock.baseUrl(), "SAND_API_KEY_TEST", "wire-bank-001", 10);
        }

        @Bean
        CircleRedemptionAdapter circleRedemptionAdapter(CircleProperties properties) {
            return new CircleRedemptionAdapter(properties);
        }
    }
}
