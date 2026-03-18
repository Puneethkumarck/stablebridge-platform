package com.stablecoin.payments.compliance.infrastructure.provider.notabene;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.travel-rule.notabene")
public record NotabeneProperties(
        String baseUrl,
        String authUrl,
        String clientId,
        String clientSecret,
        String audience,
        String vaspDid,
        int timeoutSeconds
) {
    public NotabeneProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.notabene.id";
        }
        if (authUrl == null || authUrl.isBlank()) {
            authUrl = "https://auth.notabene.id/oauth/token";
        }
        if (audience == null || audience.isBlank()) {
            audience = "https://api.notabene.id";
        }
        if (timeoutSeconds <= 0) {
            timeoutSeconds = 5;
        }
    }
}
