package com.stablecoin.payments.compliance.infrastructure.provider.worldcheck;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.stablecoin.payments.compliance.domain.model.SanctionsResult;
import com.stablecoin.payments.compliance.domain.port.SanctionsProvider;
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
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = WorldCheckSanctionsAdapterRetryTest.TestConfig.class)
@DisplayName("WorldCheckSanctionsAdapter retry behavior")
class WorldCheckSanctionsAdapterRetryTest {

    private static WireMockServer wireMock;

    @Autowired
    private SanctionsProvider sanctionsProvider;

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
        registry.add("app.sanctions.provider", () -> "world-check");
        registry.add("app.sanctions.world-check.base-url", () -> wireMock.baseUrl() + "/v2");
        registry.add("app.sanctions.world-check.api-key", () -> "test-key");
        registry.add("app.sanctions.world-check.api-secret", () -> "test-secret");
        registry.add("app.sanctions.world-check.group-id", () -> "test-group");
        registry.add("app.sanctions.world-check.timeout-seconds", () -> "2");
        registry.add("resilience4j.retry.instances.sanctions.max-attempts", () -> "3");
        registry.add("resilience4j.retry.instances.sanctions.wait-duration", () -> "10ms");
        registry.add("resilience4j.retry.instances.sanctions.retry-exceptions[0]",
                () -> "org.springframework.web.client.HttpServerErrorException");
        registry.add("resilience4j.retry.instances.sanctions.ignore-exceptions[0]",
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
        wireMock.stubFor(post(urlEqualTo("/v2/cases/screeningRequest"))
                .inScenario("retry-then-success")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("second-attempt"));

        wireMock.stubFor(post(urlEqualTo("/v2/cases/screeningRequest"))
                .inScenario("retry-then-success")
                .whenScenarioStateIs("second-attempt")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "caseId": "test-id",
                                  "caseSystemId": "WC-001",
                                  "screeningState": "SCREENED",
                                  "results": []
                                }
                                """)));

        var senderId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        var recipientId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        var result = sanctionsProvider.screen(senderId, recipientId);

        var expected = SanctionsResult.builder()
                .senderScreened(true)
                .recipientScreened(true)
                .senderHit(false)
                .recipientHit(false)
                .hitDetails(null)
                .listsChecked(List.of("OFAC_SDN", "EU_CONSOLIDATED", "UN"))
                .provider("world-check")
                .build();
        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("sanctionsResultId", "checkId", "providerRef", "screenedAt")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("should not retry on 400 client error")
    void shouldNotRetryOnClientError() {
        wireMock.stubFor(post(urlEqualTo("/v2/cases/screeningRequest"))
                .willReturn(aResponse().withStatus(400)));

        var senderId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        var recipientId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        assertThatThrownBy(() -> sanctionsProvider.screen(senderId, recipientId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Sanctions screening unavailable");

        // Should call exactly once — 400 is not retried (sender call fails, no recipient call)
        wireMock.verify(1, postRequestedFor(urlEqualTo("/v2/cases/screeningRequest")));
    }

    @Test
    @DisplayName("should exhaust retries on persistent 503 and invoke fallback")
    void shouldExhaustRetriesAndInvokeFallback() {
        wireMock.stubFor(post(urlEqualTo("/v2/cases/screeningRequest"))
                .willReturn(aResponse().withStatus(503)));

        var senderId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        var recipientId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        assertThatThrownBy(() -> sanctionsProvider.screen(senderId, recipientId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Sanctions screening unavailable");

        // max-attempts=3 => 3 calls for the first screenEntity call (sender)
        wireMock.verify(3, postRequestedFor(urlEqualTo("/v2/cases/screeningRequest")));
    }

    @Configuration
    @EnableAspectJAutoProxy
    @Import({RetryAutoConfiguration.class, CircuitBreakerAutoConfiguration.class})
    static class TestConfig {

        @Bean
        WorldCheckProperties worldCheckProperties() {
            return new WorldCheckProperties(
                    wireMock.baseUrl() + "/v2",
                    "test-key",
                    "test-secret",
                    "test-group",
                    2
            );
        }

        @Bean
        WorldCheckSanctionsAdapter worldCheckSanctionsAdapter(WorldCheckProperties properties) {
            return new WorldCheckSanctionsAdapter(properties);
        }
    }
}
