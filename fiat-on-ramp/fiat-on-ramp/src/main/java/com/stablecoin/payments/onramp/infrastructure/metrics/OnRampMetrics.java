package com.stablecoin.payments.onramp.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Custom business metrics for the Fiat On-Ramp service.
 *
 * <p>Tracks collection initiation, completion, failure, and refund events.
 */
@Component
@RequiredArgsConstructor
public class OnRampMetrics {

    private final MeterRegistry meterRegistry;

    /**
     * Records a collection initiation event.
     */
    public void recordCollectionInitiated(String psp, String currency) {
        meterRegistry.counter("onramp.collection.initiated",
                "psp", psp,
                "currency", currency
        ).increment();
    }

    /**
     * Records a collection completion event.
     */
    public void recordCollectionCompleted(String psp, String currency) {
        meterRegistry.counter("onramp.collection.completed",
                "psp", psp,
                "currency", currency
        ).increment();
    }

    /**
     * Records a collection failure event.
     */
    public void recordCollectionFailed(String psp, String reason) {
        meterRegistry.counter("onramp.collection.failed",
                "psp", psp,
                "reason", reason
        ).increment();
    }

    /**
     * Records a refund processed event.
     */
    public void recordRefundProcessed(String psp) {
        meterRegistry.counter("onramp.refund.processed",
                "psp", psp
        ).increment();
    }
}
