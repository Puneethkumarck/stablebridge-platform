package com.stablecoin.payments.compliance.infrastructure.provider.chainalysis;

import com.stablecoin.payments.compliance.domain.model.AmlResult;
import com.stablecoin.payments.compliance.domain.port.AmlProvider;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.stablecoin.payments.platform.infrastructure.http.ExternalApiLoggingInterceptor.applyTo;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.aml.provider", havingValue = "chainalysis")
@EnableConfigurationProperties(ChainalysisProperties.class)
public class ChainalysisAmlAdapter implements AmlProvider {

    private static final String HIGH_RATING = "HIGH";
    private static final String SEVERE_RATING = "SEVERE";
    private static final String HIGH_ALERT = "HIGH";
    private static final String SEVERE_ALERT = "SEVERE";

    private final RestClient restClient;

    public ChainalysisAmlAdapter(ChainalysisProperties properties,
                                 @Nullable ExternalApiLoggingInterceptor loggingInterceptor) {
        var httpClient = HttpClient.newBuilder()
                .version(Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(properties.timeoutSeconds()))
                .build();

        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));

        this.restClient = applyTo(RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Token " + properties.apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactory), loggingInterceptor)
                .build();
    }

    @Override
    @Bulkhead(name = "aml")
    @Retry(name = "aml", fallbackMethod = "analyzeFallback")
    @CircuitBreaker(name = "aml", fallbackMethod = "analyzeFallback")
    public AmlResult analyze(UUID senderId, UUID recipientId) {
        log.info("[CHAINALYSIS] Analyzing sender={} recipient={}", senderId, recipientId);

        var senderResponse = registerAndAnalyzeTransfer(senderId.toString());
        var recipientResponse = registerAndAnalyzeTransfer(recipientId.toString());

        var flagReasons = collectFlagReasons(senderResponse, recipientResponse, senderId, recipientId);
        var flagged = !flagReasons.isEmpty();
        var chainAnalysis = buildChainAnalysis(senderResponse, recipientResponse);
        var providerRef = buildProviderRef(senderId, recipientId);

        var result = AmlResult.builder()
                .amlResultId(UUID.randomUUID())
                .flagged(flagged)
                .flagReasons(flagReasons)
                .chainAnalysis(chainAnalysis)
                .provider("chainalysis")
                .providerRef(providerRef)
                .screenedAt(Instant.now())
                .build();

        if (flagged) {
            log.warn("[CHAINALYSIS] AML flags detected sender={} recipient={} reasons={}",
                    senderId, recipientId, flagReasons);
        } else {
            log.info("[CHAINALYSIS] No AML flags sender={} recipient={}", senderId, recipientId);
        }

        return result;
    }

    private ChainalysisTransferResponse registerAndAnalyzeTransfer(String userId) {
        registerTransfer(userId);
        return getTransferAnalysis(userId);
    }

    private void registerTransfer(String userId) {
        var requestBody = Map.of(
                "network", "USDC",
                "asset", "USDC",
                "transferReference", userId,
                "direction", "sent"
        );

        restClient.post()
                .uri("/users/{userId}/transfers", userId)
                .body(requestBody)
                .retrieve()
                .toBodilessEntity();
    }

    private ChainalysisTransferResponse getTransferAnalysis(String userId) {
        return restClient.get()
                .uri("/users/{userId}/transfers", userId)
                .retrieve()
                .body(ChainalysisTransferResponse.class);
    }

    private List<String> collectFlagReasons(ChainalysisTransferResponse senderResp,
                                            ChainalysisTransferResponse recipientResp,
                                            UUID senderId, UUID recipientId) {
        var reasons = new ArrayList<String>();
        addRatingReasons(reasons, senderResp, "sender", senderId);
        addRatingReasons(reasons, recipientResp, "recipient", recipientId);
        addAlertReasons(reasons, senderResp, "sender", senderId);
        addAlertReasons(reasons, recipientResp, "recipient", recipientId);
        return List.copyOf(reasons);
    }

    private void addRatingReasons(List<String> reasons, ChainalysisTransferResponse response,
                                   String party, UUID partyId) {
        if (response == null || response.rating() == null) {
            return;
        }
        if (HIGH_RATING.equals(response.rating()) || SEVERE_RATING.equals(response.rating())) {
            reasons.add("%s(%s):rating/%s".formatted(party, partyId, response.rating()));
        }
    }

    private void addAlertReasons(List<String> reasons, ChainalysisTransferResponse response,
                                 String party, UUID partyId) {
        if (response == null || response.alerts() == null) {
            return;
        }
        response.alerts().stream()
                .filter(alert -> HIGH_ALERT.equals(alert.alertLevel()) || SEVERE_ALERT.equals(alert.alertLevel()))
                .forEach(alert -> reasons.add("%s(%s):%s/%s".formatted(
                        party, partyId, alert.alertLevel(), alert.category())));
    }

    private String buildChainAnalysis(ChainalysisTransferResponse senderResp,
                                      ChainalysisTransferResponse recipientResp) {
        var sb = new StringBuilder("{");
        sb.append("\"senderRating\":\"%s\"".formatted(ratingOf(senderResp)));
        sb.append(",\"recipientRating\":\"%s\"".formatted(ratingOf(recipientResp)));
        sb.append("}");
        return sb.toString();
    }

    private String ratingOf(ChainalysisTransferResponse response) {
        return response != null && response.rating() != null ? response.rating() : "unknown";
    }

    private String buildProviderRef(UUID senderId, UUID recipientId) {
        return "chainalysis:%s/%s".formatted(senderId, recipientId);
    }

    @SuppressWarnings("unused")
    private AmlResult analyzeFallback(UUID senderId, UUID recipientId, Exception ex) {
        log.error("[CHAINALYSIS] Resilience fallback — AML analysis failed sender={} recipient={} due to {}",
                senderId, recipientId, ex.getClass().getSimpleName(), ex);
        throw new IllegalStateException("AML screening unavailable", ex);
    }
}
