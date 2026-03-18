package com.stablecoin.payments.compliance.infrastructure.provider.notabene;

import com.stablecoin.payments.compliance.domain.model.TravelRulePackage;
import com.stablecoin.payments.compliance.domain.port.TravelRuleProvider;
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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.travel-rule.provider", havingValue = "notabene")
@EnableConfigurationProperties(NotabeneProperties.class)
public class NotabeneTravelRuleAdapter implements TravelRuleProvider {

    private static final long TOKEN_EXPIRY_BUFFER_SECONDS = 300;

    private final RestClient apiClient;
    private final RestClient authClient;
    private final NotabeneProperties properties;
    private final ReentrantLock tokenLock = new ReentrantLock();

    private volatile String cachedToken;
    private volatile Instant tokenExpiresAt = Instant.EPOCH;

    public NotabeneTravelRuleAdapter(NotabeneProperties properties) {
        this.properties = properties;

        var httpClient = HttpClient.newBuilder()
                .version(Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(properties.timeoutSeconds()))
                .build();

        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(properties.timeoutSeconds()));

        this.apiClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactory)
                .build();

        this.authClient = RestClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    @Retry(name = "travel-rule", fallbackMethod = "transmitFallback")
    @CircuitBreaker(name = "travel-rule", fallbackMethod = "transmitFallback")
    public String transmit(TravelRulePackage travelRulePackage) {
        log.info("[NOTABENE] Transmitting travel rule package={} check={}",
                travelRulePackage.packageId(), travelRulePackage.checkId());

        var token = getValidToken();
        var request = mapToRequest(travelRulePackage);

        var transferUri = URI.create(properties.baseUrl() + "/entities/" + properties.vaspDid() + "/tx");
        var response = apiClient.post()
                .uri(transferUri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(request)
                .retrieve()
                .body(NotabeneTransferResponse.class);

        if (response == null) {
            throw new IllegalStateException("Notabene returned null response");
        }

        log.info("[NOTABENE] Transfer created id={} status={} ref={}",
                response.id(), response.status(), response.transactionRef());

        return response.id();
    }

    String getValidToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt)) {
            return cachedToken;
        }

        tokenLock.lock();
        try {
            if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt)) {
                return cachedToken;
            }
            return refreshToken();
        } finally {
            tokenLock.unlock();
        }
    }

    private String refreshToken() {
        log.info("[NOTABENE] Refreshing OAuth2 access token");

        var tokenRequest = new NotabeneTokenRequest(
                properties.clientId(),
                properties.clientSecret(),
                "client_credentials",
                properties.audience()
        );

        var tokenResponse = authClient.post()
                .uri(properties.authUrl())
                .body(tokenRequest)
                .retrieve()
                .body(NotabeneTokenResponse.class);

        if (tokenResponse == null || tokenResponse.accessToken() == null) {
            throw new IllegalStateException("Notabene OAuth2 token response is null or missing access_token");
        }

        cachedToken = tokenResponse.accessToken();
        int expiresIn = tokenResponse.expiresIn() != null ? tokenResponse.expiresIn() : 86400;
        tokenExpiresAt = Instant.now().plusSeconds(expiresIn - TOKEN_EXPIRY_BUFFER_SECONDS);

        log.info("[NOTABENE] OAuth2 token refreshed, expires in {}s", expiresIn);

        return cachedToken;
    }

    private NotabeneCreateTransferRequest mapToRequest(TravelRulePackage pkg) {
        var originatorName = new NotabeneCreateTransferRequest.NameIdentifier(
                extractField(pkg.originatorData(), "primaryIdentifier", "Unknown"),
                extractField(pkg.originatorData(), "secondaryIdentifier", "Unknown")
        );

        var beneficiaryName = new NotabeneCreateTransferRequest.NameIdentifier(
                extractField(pkg.beneficiaryData(), "primaryIdentifier", "Unknown"),
                extractField(pkg.beneficiaryData(), "secondaryIdentifier", "Unknown")
        );

        var originatorAddress = new NotabeneCreateTransferRequest.GeographicAddress(
                List.of(extractField(pkg.originatorData(), "address", "")),
                pkg.originatorVasp() != null ? pkg.originatorVasp().country() : ""
        );

        var originator = new NotabeneCreateTransferRequest.Originator(
                List.of(new NotabeneCreateTransferRequest.OriginatorPerson(
                        new NotabeneCreateTransferRequest.NaturalPerson(
                                List.of(new NotabeneCreateTransferRequest.Name(List.of(originatorName))),
                                List.of(originatorAddress)
                        )
                )),
                List.of(extractField(pkg.originatorData(), "accountNumber", ""))
        );

        var beneficiary = new NotabeneCreateTransferRequest.Beneficiary(
                List.of(new NotabeneCreateTransferRequest.BeneficiaryPerson(
                        new NotabeneCreateTransferRequest.NaturalPerson(
                                List.of(new NotabeneCreateTransferRequest.Name(List.of(beneficiaryName))),
                                null
                        )
                )),
                List.of(extractField(pkg.beneficiaryData(), "accountNumber", ""))
        );

        return new NotabeneCreateTransferRequest(
                pkg.packageId().toString(),
                "USDC",
                extractField(pkg.originatorData(), "amount", "0"),
                pkg.originatorVasp() != null ? pkg.originatorVasp().did() : properties.vaspDid(),
                pkg.beneficiaryVasp() != null ? pkg.beneficiaryVasp().did() : "",
                originator,
                beneficiary,
                null
        );
    }

    private String extractField(String jsonData, String fieldName, String defaultValue) {
        if (jsonData == null || jsonData.isBlank() || "{}".equals(jsonData.trim())) {
            return defaultValue;
        }
        var searchKey = "\"" + fieldName + "\"";
        int keyIndex = jsonData.indexOf(searchKey);
        if (keyIndex < 0) {
            return defaultValue;
        }
        int colonIndex = jsonData.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex < 0) {
            return defaultValue;
        }
        int valueStart = colonIndex + 1;
        while (valueStart < jsonData.length() && jsonData.charAt(valueStart) == ' ') {
            valueStart++;
        }
        if (valueStart >= jsonData.length()) {
            return defaultValue;
        }
        if (jsonData.charAt(valueStart) == '"') {
            int valueEnd = jsonData.indexOf('"', valueStart + 1);
            if (valueEnd < 0) {
                return defaultValue;
            }
            return jsonData.substring(valueStart + 1, valueEnd);
        }
        int valueEnd = valueStart;
        while (valueEnd < jsonData.length()
                && jsonData.charAt(valueEnd) != ','
                && jsonData.charAt(valueEnd) != '}') {
            valueEnd++;
        }
        return jsonData.substring(valueStart, valueEnd).trim();
    }

    @SuppressWarnings("unused")
    private String transmitFallback(TravelRulePackage travelRulePackage, Exception ex) {
        log.error("[NOTABENE] Resilience fallback — travel rule transmission failed package={} due to {}",
                travelRulePackage.packageId(), ex.getClass().getSimpleName(), ex);
        throw new IllegalStateException("Travel rule transmission unavailable", ex);
    }
}
