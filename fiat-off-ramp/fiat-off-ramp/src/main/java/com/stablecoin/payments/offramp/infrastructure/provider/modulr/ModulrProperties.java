package com.stablecoin.payments.offramp.infrastructure.provider.modulr;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Modulr payout integration.
 *
 * @param baseUrl         Modulr API base URL (sandbox: https://api-sandbox.modulrfinance.com)
 * @param apiKey          Modulr API key (Bearer token for sandbox)
 * @param apiSecret       Modulr API secret (used for HMAC auth in production)
 * @param sourceAccountId Modulr source account ID for outgoing payments
 * @param timeoutSeconds  HTTP client timeout in seconds (default 10)
 */
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
            apiKey = "SANDBOX_API_KEY_DEFAULT";
        }
        if (apiSecret == null || apiSecret.isBlank()) {
            apiSecret = "SANDBOX_API_SECRET_DEFAULT";
        }
        if (sourceAccountId == null || sourceAccountId.isBlank()) {
            sourceAccountId = "A1100ABCD1";
        }
        if (timeoutSeconds <= 0) {
            timeoutSeconds = 10;
        }
    }
}
