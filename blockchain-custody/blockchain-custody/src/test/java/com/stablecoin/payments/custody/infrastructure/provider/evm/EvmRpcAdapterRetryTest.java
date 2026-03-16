package com.stablecoin.payments.custody.infrastructure.provider.evm;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.stablecoin.payments.custody.domain.model.ChainId;
import com.stablecoin.payments.custody.domain.port.ChainRpcProvider;
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

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = EvmRpcAdapterRetryTest.TestConfig.class)
@DisplayName("EvmRpcAdapter retry behavior")
class EvmRpcAdapterRetryTest {

    private static WireMockServer wireMock;
    private static final ChainId BASE_CHAIN = new ChainId("base");

    @Autowired
    private ChainRpcProvider chainRpcProvider;

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
        registry.add("app.custody.evm.enabled", () -> "true");
        registry.add("resilience4j.retry.instances.evmRpc.max-attempts", () -> "3");
        registry.add("resilience4j.retry.instances.evmRpc.wait-duration", () -> "10ms");
        registry.add("resilience4j.retry.instances.evmRpc.retry-exceptions[0]",
                () -> "org.springframework.web.client.HttpServerErrorException");
        registry.add("resilience4j.retry.instances.evmRpc.ignore-exceptions[0]",
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
        wireMock.stubFor(post(urlEqualTo("/"))
                .inScenario("retry-then-success")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("second-attempt"));

        wireMock.stubFor(post(urlEqualTo("/"))
                .inScenario("retry-then-success")
                .whenScenarioStateIs("second-attempt")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"jsonrpc":"2.0","id":1,"result":"0x1a4"}
                                """)));

        var result = chainRpcProvider.getLatestBlockNumber(BASE_CHAIN);

        assertThat(result).isEqualTo(420L);
    }

    @Test
    @DisplayName("should not retry on 400 client error")
    void shouldNotRetryOnClientError() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse().withStatus(400)));

        assertThatThrownBy(() -> chainRpcProvider.getLatestBlockNumber(BASE_CHAIN))
                .isInstanceOf(Exception.class);

        wireMock.verify(1, postRequestedFor(urlEqualTo("/")));
    }

    @Test
    @DisplayName("should exhaust retries on persistent 503 and invoke fallback")
    void shouldExhaustRetriesAndInvokeFallback() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> chainRpcProvider.getLatestBlockNumber(BASE_CHAIN))
                .isInstanceOf(Exception.class);

        wireMock.verify(3, postRequestedFor(urlEqualTo("/")));
    }

    @Configuration
    @EnableAspectJAutoProxy
    @Import({RetryAutoConfiguration.class, CircuitBreakerAutoConfiguration.class})
    static class TestConfig {

        @Bean
        EvmChainProperties evmChainProperties() {
            return new EvmChainProperties(true, Map.of(
                    "base", new EvmChainProperties.ChainRpcConfig(
                            wireMock.baseUrl(), 84532L,
                            "0x036CbD53842c5426634e7929541eC2318f3dCF7e",
                            5000, 10000)
            ));
        }

        @Bean
        EvmRpcAdapter evmRpcAdapter(EvmChainProperties properties) {
            return new EvmRpcAdapter(properties);
        }
    }
}
