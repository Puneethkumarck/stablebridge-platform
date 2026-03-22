package com.stablecoin.payments.compliance.infrastructure.provider.persona;

import com.stablecoin.payments.compliance.domain.model.KycResult;
import com.stablecoin.payments.compliance.domain.model.KycStatus;
import com.stablecoin.payments.compliance.domain.model.KycTier;
import com.stablecoin.payments.compliance.domain.port.KycProvider;
import com.stablecoin.payments.platform.infrastructure.http.ExternalApiLoggingInterceptor;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static com.stablecoin.payments.platform.infrastructure.http.ExternalApiLoggingInterceptor.applyTo;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.kyc.provider", havingValue = "persona")
@EnableConfigurationProperties(PersonaProperties.class)
public class PersonaKycAdapter implements KycProvider {

    private final RestClient restClient;
    private final PersonaProperties properties;

    public PersonaKycAdapter(PersonaProperties properties,
                             @Nullable ExternalApiLoggingInterceptor loggingInterceptor) {
        this.properties = properties;

        var httpClient = HttpClient.newBuilder()
                .version(Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(properties.timeoutSeconds()))
                .build();

        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));

        this.restClient = applyTo(RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Persona-Version", properties.personaVersion())
                .requestFactory(requestFactory), loggingInterceptor)
                .build();
    }

    @Override
    @Bulkhead(name = "kyc")
    @Retry(name = "kyc", fallbackMethod = "verifyFallback")
    @CircuitBreaker(name = "kyc", fallbackMethod = "verifyFallback")
    public KycResult verify(UUID senderId, UUID recipientId) {
        log.info("[PERSONA] KYC verification sender={} recipient={}", senderId, recipientId);

        var senderResult = createAndCheckInquiry(senderId);
        var recipientResult = createAndCheckInquiry(recipientId);

        var result = KycResult.builder()
                .kycResultId(UUID.randomUUID())
                .senderKycTier(senderResult.tier())
                .senderStatus(senderResult.status())
                .recipientStatus(recipientResult.status())
                .provider("persona")
                .providerRef("persona:" + senderId + "/" + recipientId)
                .checkedAt(Instant.now())
                .build();

        log.info("[PERSONA] KYC result: senderStatus={} recipientStatus={}",
                senderResult.status(), recipientResult.status());
        return result;
    }

    private CustomerKycResult createAndCheckInquiry(UUID customerId) {
        var request = PersonaCreateInquiryRequest.of(
                properties.inquiryTemplateId(), customerId.toString());

        var response = restClient.post()
                .uri("/api/v1/inquiries")
                .body(request)
                .retrieve()
                .body(PersonaInquiryResponse.class);

        if (response == null || response.data() == null) {
            log.warn("[PERSONA] Empty response for customer={}", customerId);
            return new CustomerKycResult(KycStatus.UNVERIFIED, KycTier.KYC_TIER_1);
        }

        var inquiryId = response.data().id();
        var status = response.data().status();
        log.info("[PERSONA] Inquiry created inquiryId={} status={} customer={}", inquiryId, status, customerId);

        return mapStatus(status);
    }

    private CustomerKycResult mapStatus(String status) {
        if (status == null) {
            return new CustomerKycResult(KycStatus.UNVERIFIED, KycTier.KYC_TIER_1);
        }
        return switch (status) {
            case "approved", "completed" -> new CustomerKycResult(KycStatus.VERIFIED, KycTier.KYC_TIER_2);
            case "created", "pending", "needs_review" -> new CustomerKycResult(KycStatus.PENDING, KycTier.KYC_TIER_1);
            case "declined" -> new CustomerKycResult(KycStatus.REJECTED, KycTier.KYC_TIER_1);
            default -> new CustomerKycResult(KycStatus.UNVERIFIED, KycTier.KYC_TIER_1);
        };
    }

    @SuppressWarnings("unused")
    private KycResult verifyFallback(UUID senderId, UUID recipientId, Exception ex) {
        log.error("[PERSONA] Resilience fallback — KYC verification failed sender={} recipient={} due to {}",
                senderId, recipientId, ex.getClass().getSimpleName(), ex);
        throw new IllegalStateException("KYC verification unavailable", ex);
    }

    private record CustomerKycResult(KycStatus status, KycTier tier) {}
}
