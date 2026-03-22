package com.stablecoin.payments.compliance.infrastructure.provider.onfido;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.stablecoin.payments.compliance.domain.model.KycResult;
import com.stablecoin.payments.compliance.domain.model.KycStatus;
import com.stablecoin.payments.compliance.domain.model.KycTier;
import com.stablecoin.payments.compliance.domain.port.KycProvider;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = OnfidoKycAdapterRetryTest.TestConfig.class)
@DisplayName("OnfidoKycAdapter retry behavior")
class OnfidoKycAdapterRetryTest {

    private static WireMockServer wireMock;
    private static StringRedisTemplate redisTemplate;
    private static ValueOperations<String, String> valueOps;

    @Autowired
    private KycProvider kycProvider;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private static final UUID SENDER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID RECIPIENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeAll
    @SuppressWarnings("unchecked")
    static void startWireMock() {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
        redisTemplate = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        given(redisTemplate.opsForValue()).willReturn(valueOps);
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("app.kyc.provider", () -> "onfido");
        registry.add("resilience4j.retry.instances.kyc.max-attempts", () -> "3");
        registry.add("resilience4j.retry.instances.kyc.wait-duration", () -> "10ms");
        registry.add("resilience4j.retry.instances.kyc.retry-exceptions[0]",
                () -> "org.springframework.web.client.HttpServerErrorException");
        registry.add("resilience4j.retry.instances.kyc.ignore-exceptions[0]",
                () -> "org.springframework.web.client.HttpClientErrorException");
        registry.add("resilience4j.retry.retry-aspect-order", () -> "3");
        registry.add("resilience4j.circuitbreaker.circuit-breaker-aspect-order", () -> "1");
    }

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        given(valueOps.get("kyc:status:" + SENDER_ID)).willReturn(null);
        given(valueOps.get("kyc:status:" + RECIPIENT_ID)).willReturn(null);
        circuitBreakerRegistry.getAllCircuitBreakers()
                .forEach(cb -> cb.transitionToClosedState());
    }

    @Test
    @DisplayName("should retry on transient 503 failure then succeed")
    void shouldRetryOnTransientFailureThenSucceed() {
        wireMock.stubFor(get(urlPathEqualTo("/v3.6/checks"))
                .inScenario("retry-then-success")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("second-attempt"));

        wireMock.stubFor(get(urlPathEqualTo("/v3.6/checks"))
                .inScenario("retry-then-success")
                .whenScenarioStateIs("second-attempt")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "checks": [{
                                    "id": "chk-001",
                                    "status": "complete",
                                    "result": "clear",
                                    "applicant_id": "app-001",
                                    "report_ids": ["rpt-001"]
                                  }]
                                }
                                """)));

        var result = kycProvider.verify(SENDER_ID, RECIPIENT_ID);

        var expected = KycResult.builder()
                .senderKycTier(KycTier.KYC_TIER_2)
                .senderStatus(KycStatus.VERIFIED)
                .recipientStatus(KycStatus.VERIFIED)
                .provider("onfido")
                .build();
        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("kycResultId", "checkId", "providerRef", "checkedAt")
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("should not retry on 400 client error")
    void shouldNotRetryOnClientError() {
        wireMock.stubFor(get(urlPathEqualTo("/v3.6/checks"))
                .willReturn(aResponse().withStatus(400)));

        assertThatThrownBy(() -> kycProvider.verify(SENDER_ID, RECIPIENT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("KYC verification unavailable");

        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/v3.6/checks")));
    }

    @Test
    @DisplayName("should exhaust retries on persistent 503 and invoke fallback")
    void shouldExhaustRetriesAndInvokeFallback() {
        wireMock.stubFor(get(urlPathEqualTo("/v3.6/checks"))
                .willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> kycProvider.verify(SENDER_ID, RECIPIENT_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("KYC verification unavailable");

        wireMock.verify(3, getRequestedFor(urlPathEqualTo("/v3.6/checks")));
    }

    @Configuration
    @EnableAspectJAutoProxy
    @Import({RetryAutoConfiguration.class, CircuitBreakerAutoConfiguration.class})
    static class TestConfig {

        @Bean
        StringRedisTemplate stringRedisTemplate() {
            return redisTemplate;
        }

        @Bean
        OnfidoProperties onfidoProperties() {
            return new OnfidoProperties(
                    wireMock.baseUrl() + "/v3.6",
                    "test-token",
                    2,
                    60
            );
        }

        @Bean
        OnfidoKycAdapter onfidoKycAdapter(OnfidoProperties properties, StringRedisTemplate template) {
            return new OnfidoKycAdapter(properties, template, null);
        }
    }
}
