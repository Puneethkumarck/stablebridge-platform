package com.stablecoin.payments.offramp.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OffRampMetrics {

    private final MeterRegistry meterRegistry;

    public void recordPayoutInitiated(String partner, String currency) {
        meterRegistry.counter("offramp.payout.initiated",
                "partner", partner,
                "currency", currency
        ).increment();
    }

    public void recordPayoutCompleted(String partner, String currency) {
        meterRegistry.counter("offramp.payout.completed",
                "partner", partner,
                "currency", currency
        ).increment();
    }

    public void recordPayoutFailed(String partner, String reason) {
        meterRegistry.counter("offramp.payout.failed",
                "partner", partner,
                "reason", reason
        ).increment();
    }

    public void recordRedemptionCompleted(String provider) {
        meterRegistry.counter("offramp.redemption.completed",
                "provider", provider
        ).increment();
    }
}
