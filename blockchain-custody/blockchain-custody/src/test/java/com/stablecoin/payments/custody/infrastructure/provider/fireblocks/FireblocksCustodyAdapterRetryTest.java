package com.stablecoin.payments.custody.infrastructure.provider.fireblocks;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.stablecoin.payments.custody.domain.port.CustodyEngine;
import com.stablecoin.payments.custody.domain.port.SignResult;
import com.stablecoin.payments.custody.domain.port.TransactionStatus;
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

import java.security.KeyPairGenerator;
import java.util.Base64;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.stablecoin.payments.custody.fixtures.CustodyEngineFixtures.aSignRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = FireblocksCustodyAdapterRetryTest.TestConfig.class)
@DisplayName("FireblocksCustodyAdapter retry behavior")
class FireblocksCustodyAdapterRetryTest {

    private static WireMockServer wireMock;
    private static String testPrivateKeyPem;

    @Autowired
    private CustodyEngine custodyEngine;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeAll
    static void startWireMock() throws Exception {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();

        var keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        var keyPair = keyGen.generateKeyPair();
        var privateKeyBytes = keyPair.getPrivate().getEncoded();
        var base64Key = Base64.getEncoder().encodeToString(privateKeyBytes);
        testPrivateKeyPem = "-----BEGIN PRIVATE KEY-----\n" + base64Key + "\n-----END PRIVATE KEY-----";
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("app.custody.provider", () -> "fireblocks");
        registry.add("resilience4j.retry.instances.fireblocks.max-attempts", () -> "3");
        registry.add("resilience4j.retry.instances.fireblocks.wait-duration", () -> "10ms");
        registry.add("resilience4j.retry.instances.fireblocks.retry-exceptions[0]",
                () -> "org.springframework.web.client.HttpServerErrorException");
        registry.add("resilience4j.retry.instances.fireblocks.ignore-exceptions[0]",
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
        wireMock.stubFor(post(urlEqualTo("/v1/transactions"))
                .inScenario("retry-then-success")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("second-attempt"));

        wireMock.stubFor(post(urlEqualTo("/v1/transactions"))
                .inScenario("retry-then-success")
                .whenScenarioStateIs("second-attempt")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": "fb-tx-001",
                                  "status": "SUBMITTED"
                                }
                                """)));

        var result = custodyEngine.signAndSubmit(aSignRequest());

        var expected = new SignResult(null, "fb-tx-001");
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    @DisplayName("should not retry on 400 client error")
    void shouldNotRetryOnClientError() {
        wireMock.stubFor(post(urlEqualTo("/v1/transactions"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "message": "Invalid asset ID",
                                  "code": 1000
                                }
                                """)));

        assertThatThrownBy(() -> custodyEngine.signAndSubmit(aSignRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Fireblocks custody unavailable");

        wireMock.verify(1, postRequestedFor(urlEqualTo("/v1/transactions")));
    }

    @Test
    @DisplayName("should exhaust retries on persistent 503 and invoke fallback")
    void shouldExhaustRetriesAndInvokeFallback() {
        wireMock.stubFor(post(urlEqualTo("/v1/transactions"))
                .willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> custodyEngine.signAndSubmit(aSignRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Fireblocks custody unavailable");

        wireMock.verify(3, postRequestedFor(urlEqualTo("/v1/transactions")));
    }

    @Test
    @DisplayName("should retry getTransactionStatus on transient 503 then succeed")
    void shouldRetryGetTransactionStatusOnTransientFailureThenSucceed() {
        wireMock.stubFor(get(urlPathMatching("/v1/transactions/.+"))
                .inScenario("status-retry-then-success")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("status-second-attempt"));

        wireMock.stubFor(get(urlPathMatching("/v1/transactions/.+"))
                .inScenario("status-retry-then-success")
                .whenScenarioStateIs("status-second-attempt")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": "fb-tx-001",
                                  "status": "COMPLETED",
                                  "txHash": "0xabc123",
                                  "numOfConfirmations": 12
                                }
                                """)));

        var result = custodyEngine.getTransactionStatus("fb-tx-001");

        var expected = new TransactionStatus("COMPLETED", "0xabc123", 12);
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    @DisplayName("should not retry getTransactionStatus on 400 client error")
    void shouldNotRetryGetTransactionStatusOnClientError() {
        wireMock.stubFor(get(urlPathMatching("/v1/transactions/.+"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "message": "Invalid transaction ID",
                                  "code": 1000
                                }
                                """)));

        assertThatThrownBy(() -> custodyEngine.getTransactionStatus("invalid-tx"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Fireblocks custody unavailable");

        wireMock.verify(1, getRequestedFor(urlPathMatching("/v1/transactions/.+")));
    }

    @Test
    @DisplayName("should exhaust retries on persistent 503 for getTransactionStatus")
    void shouldExhaustRetriesForGetTransactionStatus() {
        wireMock.stubFor(get(urlPathMatching("/v1/transactions/.+"))
                .willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> custodyEngine.getTransactionStatus("fb-tx-001"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Fireblocks custody unavailable");

        wireMock.verify(3, getRequestedFor(urlPathMatching("/v1/transactions/.+")));
    }

    @Configuration
    @EnableAspectJAutoProxy
    @Import({RetryAutoConfiguration.class, CircuitBreakerAutoConfiguration.class})
    static class TestConfig {

        @Bean
        FireblocksProperties fireblocksProperties() {
            return new FireblocksProperties(
                    wireMock.baseUrl(),
                    "fb-test-api-key",
                    testPrivateKeyPem,
                    "42",
                    10
            );
        }

        @Bean
        FireblocksCustodyAdapter fireblocksCustodyAdapter(FireblocksProperties properties) {
            return new FireblocksCustodyAdapter(properties);
        }
    }
}
