package com.stablecoin.payments.offramp.application.config;

import com.stablecoin.payments.offramp.domain.port.PayoutMonitorProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payout.monitor")
public record PayoutMonitorConfig(
        boolean enabled,
        long intervalMs,
        int stuckThresholdMinutes
) implements PayoutMonitorProperties {

    public PayoutMonitorConfig {
        if (intervalMs <= 0) {
            intervalMs = 300_000;
        }
        if (stuckThresholdMinutes <= 0) {
            stuckThresholdMinutes = 120;
        }
    }
}
