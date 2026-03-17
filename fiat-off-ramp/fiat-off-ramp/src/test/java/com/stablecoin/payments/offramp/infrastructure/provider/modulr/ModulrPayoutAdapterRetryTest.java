package com.stablecoin.payments.offramp.infrastructure.provider.modulr;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.stablecoin.payments.offramp.domain.model.AccountType;
import com.stablecoin.payments.offramp.domain.model.BankAccount;
import com.stablecoin.payments.offramp.domain.model.PartnerIdentifier;
import com.stablecoin.payments.offramp.domain.model.PaymentRail;
import com.stablecoin.payments.offramp.domain.exception.PayoutPartnerException;
import com.stablecoin.payments.offramp.domain.port.PayoutPartnerGateway;
import com.stablecoin.payments.offramp.domain.port.PayoutRequest;
import com.stablecoin.payments.offramp.domain.port.PayoutResult;
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

@SpringBootTest(classes = ModulrPayoutAdapterRetryTest.TestConfig.class)
@DisplayName("ModulrPayoutAdapter retry behavior")
class ModulrPayoutAdapterRetryTest {

    private static WireMockServer wireMock;
    private static final UUID PAYOUT_ID = UUID.fromString("887adb57-1d2e-4f3a-b5c6-d7e8f9a0b1c2");

    @Autowired
    private PayoutPartnerGateway payoutPartnerGateway;

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
        registry.add("app.payout.provider", () -> "modulr");
        registry.add("resilience4j.retry.instances.modulr.max-attempts", () -> "3");
        registry.add("resilience4j.retry.instances.modulr.wait-duration", () -> "10ms");
        registry.add("resilience4j.retry.instances.modulr.retry-exceptions[0]",
                () -> "org.springframework.web.client.HttpServerErrorException");
        registry.add("resilience4j.retry.instances.modulr.ignore-exceptions[0]",
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

    private PayoutRequest aPayoutRequest() {
        return new PayoutRequest(
                PAYOUT_ID,
                new BigDecimal("9500.00"),
                "EUR",
                new BankAccount("DE89370400440532013000", "DEUTDEFF", AccountType.IBAN, "DE"),
                null,
                PaymentRail.SEPA,
                new PartnerIdentifier("modulr-001", "modulr")
        );
    }

    @Test
    @DisplayName("should retry on transient 503 failure then succeed")
    void shouldRetryOnTransientFailureThenSucceed() {
        wireMock.stubFor(post(urlEqualTo("/api-sandbox-token/payments"))
                .inScenario("retry-then-success")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("second-attempt"));

        wireMock.stubFor(post(urlEqualTo("/api-sandbox-token/payments"))
                .inScenario("retry-then-success")
                .whenScenarioStateIs("second-attempt")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": "P120003AQM",
                                  "status": "VALIDATED",
                                  "createdDate": "2026-03-15T10:30:00.000+0000",
                                  "externalReference": "887adb57-1d2e-4f3a-b5c6-d7e8f9a0b1c2",
                                  "approvalStatus": "NOTNEEDED",
                                  "message": ""
                                }
                                """)));

        var result = payoutPartnerGateway.initiatePayout(aPayoutRequest());

        var expected = new PayoutResult("P120003AQM", "VALIDATED", null);
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    @DisplayName("should not retry on 400 client error")
    void shouldNotRetryOnClientError() {
        wireMock.stubFor(post(urlEqualTo("/api-sandbox-token/payments"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "field": "amount",
                                  "code": "INVALID",
                                  "message": "Amount must be positive"
                                }
                                """)));

        assertThatThrownBy(() -> payoutPartnerGateway.initiatePayout(aPayoutRequest()))
                .isInstanceOf(PayoutPartnerException.class);

        wireMock.verify(1, postRequestedFor(urlEqualTo("/api-sandbox-token/payments")));
    }

    @Test
    @DisplayName("should exhaust retries on persistent 503 and invoke fallback")
    void shouldExhaustRetriesAndInvokeFallback() {
        wireMock.stubFor(post(urlEqualTo("/api-sandbox-token/payments"))
                .willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> payoutPartnerGateway.initiatePayout(aPayoutRequest()))
                .isInstanceOf(PayoutPartnerException.class);

        wireMock.verify(3, postRequestedFor(urlEqualTo("/api-sandbox-token/payments")));
    }

    @Configuration
    @EnableAspectJAutoProxy
    @Import({RetryAutoConfiguration.class, CircuitBreakerAutoConfiguration.class})
    static class TestConfig {

        @Bean
        ModulrProperties modulrProperties() {
            return new ModulrProperties(
                    wireMock.baseUrl(), "SANDBOX_TEST_API_KEY", "test-secret", "A1100ABCD1", 10);
        }

        @Bean
        ModulrPayoutAdapter modulrPayoutAdapter(ModulrProperties properties) {
            return new ModulrPayoutAdapter(properties);
        }
    }
}
