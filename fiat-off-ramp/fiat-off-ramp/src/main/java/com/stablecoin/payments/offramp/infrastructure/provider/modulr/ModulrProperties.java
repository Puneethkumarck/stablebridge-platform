package com.stablecoin.payments.offramp.infrastructure.provider.modulr;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payout.modulr")
public record ModulrProperties(
        String baseUrl,
        String apiKey,
        String apiSecret,
        String sourceAccountId,
        int timeoutSeconds
) {

    public ModulrProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api-sandbox.modulrfinance.com";
        }
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = "";
        }
        if (apiSecret == null || apiSecret.isBlank()) {
            apiSecret = "";
        }
        if (sourceAccountId == null || sourceAccountId.isBlank()) {
            sourceAccountId = "";
        }
        if (timeoutSeconds <= 0) {
            timeoutSeconds = 10;
        }
    }
}
