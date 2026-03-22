package com.stablecoin.payments.compliance.infrastructure.provider.chainalysis;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.stablecoin.payments.compliance.domain.model.AmlResult;
import com.stablecoin.payments.compliance.domain.port.AmlProvider;
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

import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = ChainalysisAmlAdapterRetryTest.TestConfig.class)
@DisplayName("ChainalysisAmlAdapter retry behavior")
class ChainalysisAmlAdapterRetryTest {

    private static WireMockServer wireMock;

    @Autowired
    private AmlProvider amlProvider;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private static final UUID SENDER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID RECIPIENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

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
        registry.add("app.aml.provider", () -> "chainalysis");
        registry.add("resilience4j.retry.instances.aml.max-attempts", () -> "3");
        registry.add("resilience4j.retry.instances.aml.wait-duration", () -> "10ms");
        registry.add("resilience4j.retry.instances.aml.retry-exceptions[0]",
                () -> "org.springframework.web.client.HttpServerErrorException");
        registry.add("resilience4j.retry.instances.aml.ignore-exceptions[0]",
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
        // Register transfer always succeeds
        wireMock.stubFor(post(urlPathMatching("/v2/users/.+/transfers"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"externalId\":\"transfer-registered\"}")));

        // First GET fails with 503, second succeeds
        wireMock.stubFor(get(urlPathMatching("/v2/users/.+/transfers"))
                .inScenario("retry-then-success")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("second-attempt"));

        wireMock.stubFor(get(urlPathMatching("/v2/users/.+/transfers"))
                .inScenario("retry-then-success")
                .whenScenarioStateIs("second-attempt")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "updatedAt": "2026-03-08T00:00:00Z",
                                  "asset": "USDC",
                                  "rating": "LOW",
                                  "alerts": []
                                }
                                """)));

        var result = amlProvider.analyze(SENDER_ID, RECIPIENT_ID);

        var expected = AmlResult.builder()
                .flagged(false)
                .flagReasons(List.of())
                .provider("chainalysis")
                .providerRef("chainalysis:%s/%s".formatted(SENDER_ID, RECIPIENT_ID))
                .build();
        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("amlResultId", "checkId", "screenedAt", "chainAnalysis")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("should not retry on 400 client error")
    void shouldNotRetryOnClientError() {
        wireMock.stubFor(post(urlPathMatching("/v2/users/.+/transfers"))
                .willReturn(aResponse().withStatus(400)));

        assertThatThrownBy(() -> amlProvider.analyze(SENDER_ID, RECIPIENT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("AML screening unavailable");

        // 400 is not retried — exactly 1 POST call
        wireMock.verify(1, postRequestedFor(urlPathMatching("/v2/users/.+/transfers")));
    }

    @Test
    @DisplayName("should exhaust retries on persistent 503 and invoke fallback")
    void shouldExhaustRetriesAndInvokeFallback() {
        wireMock.stubFor(post(urlPathMatching("/v2/users/.+/transfers"))
                .willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> amlProvider.analyze(SENDER_ID, RECIPIENT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("AML screening unavailable");

        // max-attempts=3 => 3 calls for the first registerTransfer call
        wireMock.verify(3, postRequestedFor(urlPathMatching("/v2/users/.+/transfers")));
    }

    @Configuration
    @EnableAspectJAutoProxy
    @Import({RetryAutoConfiguration.class, CircuitBreakerAutoConfiguration.class})
    static class TestConfig {

        @Bean
        ChainalysisProperties chainalysisProperties() {
            return new ChainalysisProperties(
                    wireMock.baseUrl() + "/v2",
                    "test-api-key",
                    2
            );
        }

        @Bean
        ChainalysisAmlAdapter chainalysisAmlAdapter(ChainalysisProperties properties) {
            return new ChainalysisAmlAdapter(properties, null);
        }
    }
}
