package com.stablecoin.payments.compliance.infrastructure.provider.ofacsdn;

import com.stablecoin.payments.platform.infrastructure.http.ExternalApiLoggingInterceptor;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.time.Duration;
import java.util.List;

import static com.stablecoin.payments.platform.infrastructure.http.ExternalApiLoggingInterceptor.applyTo;

@Slf4j
@Component
class SdnListDownloader {

    private final RestClient restClient;
    private final String sdnFileName;

    SdnListDownloader(OfacSdnProperties properties,
                      @Nullable ExternalApiLoggingInterceptor loggingInterceptor) {
        this.sdnFileName = properties.sdnFileName();

        var httpClient = HttpClient.newBuilder()
                .version(Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(properties.connectionTimeoutMs()))
                .build();

        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));

        this.restClient = applyTo(RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory), loggingInterceptor)
                .build();
    }

    @Bulkhead(name = "sanctions")
    @Retry(name = "sanctions", fallbackMethod = "downloadFallback")
    @CircuitBreaker(name = "sanctions", fallbackMethod = "downloadFallback")
    List<SdnEntry> download() {
        log.info("[OFAC-SDN] Downloading SDN list fileName={}", sdnFileName);

        var xml = restClient.get()
                .uri("/{fileName}", sdnFileName)
                .retrieve()
                .body(String.class);

        var entries = SdnXmlParser.parse(xml);
        log.info("[OFAC-SDN] SDN list loaded with {} entries", entries.size());
        return entries;
    }

    @SuppressWarnings("unused")
    private List<SdnEntry> downloadFallback(Exception ex) {
        log.error("[OFAC-SDN] Failed to download SDN list: {}", ex.getMessage(), ex);
        throw new IllegalStateException("OFAC SDN list unavailable", ex);
    }
}
