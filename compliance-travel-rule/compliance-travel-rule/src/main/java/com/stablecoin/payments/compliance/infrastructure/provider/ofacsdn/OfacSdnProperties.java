package com.stablecoin.payments.compliance.infrastructure.provider.ofacsdn;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sanctions.ofac-sdn")
public record OfacSdnProperties(
        String baseUrl,
        String sdnFileName,
        Double matchThreshold,
        Integer cacheRefreshHours,
        Integer connectionTimeoutMs,
        Integer readTimeoutMs
) {
    public OfacSdnProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://www.treasury.gov/ofac/downloads";
        }
        if (sdnFileName == null || sdnFileName.isBlank()) {
            sdnFileName = "sdn.xml";
        }
        if (matchThreshold == null || matchThreshold <= 0.0) {
            matchThreshold = 0.85;
        }
        if (cacheRefreshHours == null || cacheRefreshHours <= 0) {
            cacheRefreshHours = 24;
        }
        if (connectionTimeoutMs == null || connectionTimeoutMs <= 0) {
            connectionTimeoutMs = 10000;
        }
        if (readTimeoutMs == null || readTimeoutMs <= 0) {
            readTimeoutMs = 30000;
        }
    }
}
