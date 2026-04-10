package com.stablecoin.payments.onramp.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnRampMetrics {

    private final MeterRegistry meterRegistry;

    public void recordCollectionInitiated(String psp, String currency) {
        meterRegistry.counter("onramp.collection.initiated",
                "psp", psp,
                "currency", currency
        ).increment();
    }

    public void recordCollectionCompleted(String psp, String currency) {
        meterRegistry.counter("onramp.collection.completed",
                "psp", psp,
                "currency", currency
        ).increment();
    }

    public void recordCollectionFailed(String psp, String reason) {
        meterRegistry.counter("onramp.collection.failed",
                "psp", psp,
                "reason", reason
        ).increment();
    }

    public void recordRefundProcessed(String psp) {
        meterRegistry.counter("onramp.refund.processed",
                "psp", psp
        ).increment();
    }
}
