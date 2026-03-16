package com.stablecoin.payments.fx.infrastructure.provider.refinitiv;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.stablecoin.payments.fx.domain.model.CorridorRate;
import com.stablecoin.payments.fx.domain.port.RateProvider;
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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = RefinitivRateAdapterRetryTest.TestConfig.class)
@DisplayName("RefinitivRateAdapter retry behavior")
class RefinitivRateAdapterRetryTest {

    private static WireMockServer wireMock;

    @Autowired
    private RateProvider rateProvider;

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
        registry.add("app.fx.rate-provider", () -> "refinitiv");
        registry.add("resilience4j.retry.instances.fxRate.max-attempts", () -> "3");
        registry.add("resilience4j.retry.instances.fxRate.wait-duration", () -> "10ms");
        registry.add("resilience4j.retry.instances.fxRate.retry-exceptions[0]",
                () -> "org.springframework.web.client.HttpServerErrorException");
        registry.add("resilience4j.retry.instances.fxRate.ignore-exceptions[0]",
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

    @Test
    @DisplayName("should retry on transient 503 failure then succeed")
    void shouldRetryOnTransientFailureThenSucceed() {
        long nowMs = System.currentTimeMillis();

        wireMock.stubFor(get(urlEqualTo("/data/pricing/v1/rates/USDEUR"))
                .inScenario("retry-then-success")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("second-attempt"));

        wireMock.stubFor(get(urlEqualTo("/data/pricing/v1/rates/USDEUR"))
                .inScenario("retry-then-success")
                .whenScenarioStateIs("second-attempt")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "currencyPair": "USDEUR",
                                  "mid": 0.9200000000,
                                  "bid": 0.9195000000,
                                  "ask": 0.9205000000,
                                  "timestamp": %d
                                }
                                """.formatted(nowMs))));

        var result = rateProvider.getRate("USD", "EUR");

        assertThat(result).isPresent();
        var expected = CorridorRate.builder()
                .fromCurrency("USD")
                .toCurrency("EUR")
                .rate(new BigDecimal("0.92"))
                .spreadBps(30)
                .feeBps(30)
                .provider("refinitiv")
                .build();
        assertThat(result.get())
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .ignoringFields("ageMs")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("should not retry on 400 client error")
    void shouldNotRetryOnClientError() {
        wireMock.stubFor(get(urlEqualTo("/data/pricing/v1/rates/USDEUR"))
                .willReturn(aResponse().withStatus(400)));

        // 400 throws HttpClientErrorException which is in ignore-exceptions — no retry
        // The fallback returns Optional.empty() instead of throwing
        var result = rateProvider.getRate("USD", "EUR");
        assertThat(result).isEmpty();

        wireMock.verify(1, getRequestedFor(urlEqualTo("/data/pricing/v1/rates/USDEUR")));
    }

    @Test
    @DisplayName("should exhaust retries on persistent 503 and invoke fallback")
    void shouldExhaustRetriesAndInvokeFallback() {
        wireMock.stubFor(get(urlEqualTo("/data/pricing/v1/rates/USDEUR"))
                .willReturn(aResponse().withStatus(503)));

        // Fallback returns Optional.empty()
        var result = rateProvider.getRate("USD", "EUR");
        assertThat(result).isEmpty();

        wireMock.verify(3, getRequestedFor(urlEqualTo("/data/pricing/v1/rates/USDEUR")));
    }

    @Configuration
    @EnableAspectJAutoProxy
    @Import({RetryAutoConfiguration.class, CircuitBreakerAutoConfiguration.class})
    static class TestConfig {

        @Bean
        RefinitivProperties refinitivProperties() {
            return new RefinitivProperties(
                    wireMock.baseUrl(), "test-api-key", 2);
        }

        @Bean
        RefinitivRateAdapter refinitivRateAdapter(RefinitivProperties properties) {
            return new RefinitivRateAdapter(properties);
        }
    }
}
