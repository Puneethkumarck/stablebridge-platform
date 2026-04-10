package com.stablecoin.payments.custody.application.config;

import com.stablecoin.payments.custody.domain.port.TransferMonitorProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.transfer")
public record TransferMonitorConfig(
        int resubmitTimeoutS,
        int maxAttempts,
        int confirmingTimeoutS
) implements TransferMonitorProperties {

    public TransferMonitorConfig {
        if (resubmitTimeoutS <= 0) {
            resubmitTimeoutS = 120;
        }
        if (maxAttempts <= 0) {
            maxAttempts = 3;
        }
        if (confirmingTimeoutS <= 0) {
            confirmingTimeoutS = 300;
        }
    }
}
