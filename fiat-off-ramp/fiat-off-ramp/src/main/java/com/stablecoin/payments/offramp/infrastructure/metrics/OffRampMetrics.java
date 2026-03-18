package com.stablecoin.payments.offramp.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Custom business metrics for the Fiat Off-Ramp service.
 *
 * <p>Tracks payout initiation, completion, failure, and redemption events.
 */
@Component
@RequiredArgsConstructor
public class OffRampMetrics {

    private final MeterRegistry meterRegistry;

    /**
     * Records a payout initiation event.
     */
    public void recordPayoutInitiated(String partner, String currency) {
        meterRegistry.counter("offramp.payout.initiated",
                "partner", partner,
                "currency", currency
        ).increment();
    }

    /**
     * Records a payout completion event.
     */
    public void recordPayoutCompleted(String partner, String currency) {
        meterRegistry.counter("offramp.payout.completed",
                "partner", partner,
                "currency", currency
        ).increment();
    }

    /**
     * Records a payout failure event.
     */
    public void recordPayoutFailed(String partner, String reason) {
        meterRegistry.counter("offramp.payout.failed",
                "partner", partner,
                "reason", reason
        ).increment();
    }

    /**
     * Records a stablecoin redemption completion event.
     */
    public void recordRedemptionCompleted(String provider) {
        meterRegistry.counter("offramp.redemption.completed",
                "provider", provider
        ).increment();
    }
}
