package com.stablecoin.payments.offramp.infrastructure.provider.modulr;

import com.stablecoin.payments.offramp.domain.exception.PayoutPartnerException;
import com.stablecoin.payments.offramp.domain.model.PaymentRail;
import com.stablecoin.payments.offramp.domain.port.PayoutPartnerGateway;
import com.stablecoin.payments.offramp.domain.port.PayoutRequest;
import com.stablecoin.payments.offramp.domain.port.PayoutResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.time.Duration;

/**
 * Modulr implementation of {@link PayoutPartnerGateway} for SEPA Credit transfers.
 * <p>
 * Calls: {@code POST /api-sandbox-token/payments}
 * Auth: Bearer API key (sandbox token mode)
 * Scheme: SEPA_CREDIT with IBAN destination
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.payout.provider", havingValue = "modulr")
@EnableConfigurationProperties(ModulrProperties.class)
public class ModulrPayoutAdapter implements PayoutPartnerGateway {

    private final RestClient restClient;
    private final ModulrProperties properties;

    public ModulrPayoutAdapter(ModulrProperties properties) {
        this.properties = properties;

        var httpClient = HttpClient.newBuilder()
                .version(Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(properties.timeoutSeconds()))
                .build();

        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    @Retry(name = "modulr", fallbackMethod = "initiatePayoutFallback")
    @CircuitBreaker(name = "modulr", fallbackMethod = "initiatePayoutFallback")
    public PayoutResult initiatePayout(PayoutRequest request) {
        log.info("[MODULR] Initiating SEPA payout payoutId={} amount={} currency={}",
                request.payoutId(), request.fiatAmount(), request.currency());

        var destination = new ModulrPaymentRequest.ModulrDestination(
                "IBAN",
                request.bankAccount().accountNumber(),
                request.partnerIdentifier().partnerName()
        );
        var permittedScheme = resolvePermittedScheme(request.paymentRail());

        var modulrRequest = new ModulrPaymentRequest(
                properties.sourceAccountId(),
                request.fiatAmount(),
                request.currency(),
                "Payout " + request.payoutId(),
                request.payoutId().toString(),
                destination,
                permittedScheme
        );

        var response = restClient.post()
                .uri("/api-sandbox-token/payments")
                .body(modulrRequest)
                .retrieve()
                .body(ModulrPaymentResponse.class);

        if (response == null || response.id() == null) {
            throw new PayoutPartnerException(
                    request.payoutId(), "modulr", "Empty response from Modulr API");
        }

        log.info("[MODULR] Payout initiated payoutId={} modulrRef={} status={} approvalStatus={}",
                request.payoutId(), response.id(), response.status(), response.approvalStatus());

        return new PayoutResult(
                response.id(),
                response.status(),
                null
        );
    }

    private static String resolvePermittedScheme(PaymentRail rail) {
        return switch (rail) {
            case SEPA -> "SEPA_CREDIT";
            default -> null;
        };
    }

    @SuppressWarnings("unused")
    private PayoutResult initiatePayoutFallback(PayoutRequest request, Exception ex) {
        log.error("[MODULR] Resilience fallback — payout failed payoutId={} due to {}",
                request.payoutId(), ex.getClass().getSimpleName(), ex);
        throw new PayoutPartnerException(
                request.payoutId(), "modulr", "Modulr payout unavailable", ex);
    }
}
