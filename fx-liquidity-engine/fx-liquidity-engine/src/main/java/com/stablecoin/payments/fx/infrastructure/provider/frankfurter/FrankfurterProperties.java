package com.stablecoin.payments.fx.infrastructure.provider.frankfurter;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.fx.frankfurter")
public record FrankfurterProperties(
        String baseUrl,
        Integer readTimeoutMs
) {
    public FrankfurterProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.frankfurter.app";
        }
        if (readTimeoutMs == null || readTimeoutMs <= 0) {
            readTimeoutMs = 5000;
        }
    }
}
