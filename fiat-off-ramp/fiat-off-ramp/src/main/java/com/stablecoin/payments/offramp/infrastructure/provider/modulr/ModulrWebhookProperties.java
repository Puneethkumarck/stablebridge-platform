package com.stablecoin.payments.offramp.infrastructure.provider.modulr;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payout.modulr.webhook")
public record ModulrWebhookProperties(
        String webhookSecret,
        int toleranceSeconds
) {

    public ModulrWebhookProperties {
        if (toleranceSeconds <= 0) {
            toleranceSeconds = 300;
        }
    }
}
