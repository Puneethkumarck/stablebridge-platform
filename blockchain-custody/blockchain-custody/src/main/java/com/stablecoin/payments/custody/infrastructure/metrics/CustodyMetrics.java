package com.stablecoin.payments.custody.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustodyMetrics {

    private final MeterRegistry meterRegistry;

    public void recordTransferInitiated(String chain, String token) {
        meterRegistry.counter("custody.transfer.initiated",
                "chain", chain,
                "token", token
        ).increment();
    }

    public void recordTransferConfirmed(String chain, String token) {
        meterRegistry.counter("custody.transfer.confirmed",
                "chain", chain,
                "token", token
        ).increment();
    }

    public void recordTransferFailed(String chain, String reason) {
        meterRegistry.counter("custody.transfer.failed",
                "chain", chain,
                "reason", reason
        ).increment();
    }

    public Timer.Sample startConfirmationTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordConfirmationDuration(Timer.Sample sample, String chain) {
        sample.stop(meterRegistry.timer("custody.transfer.confirmation.duration", "chain", chain));
    }
}
