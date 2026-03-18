package com.stablecoin.payments.compliance.infrastructure.provider.persona;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kyc.persona")
public record PersonaProperties(
        String baseUrl,
        String apiKey,
        String inquiryTemplateId,
        String personaVersion,
        int timeoutSeconds
) {
    public PersonaProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.withpersona.com";
        }
        if (personaVersion == null || personaVersion.isBlank()) {
            personaVersion = "2023-01-05";
        }
        if (timeoutSeconds <= 0) {
            timeoutSeconds = 15;
        }
    }
}
