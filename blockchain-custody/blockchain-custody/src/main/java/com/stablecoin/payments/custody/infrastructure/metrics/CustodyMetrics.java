package com.stablecoin.payments.custody.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Custom business metrics for the Blockchain and Custody service.
 *
 * <p>Tracks transfer initiation, confirmation, failure, and confirmation duration.
 */
@Component
@RequiredArgsConstructor
public class CustodyMetrics {

    private final MeterRegistry meterRegistry;

    /**
     * Records a chain transfer initiation event.
     */
    public void recordTransferInitiated(String chain, String token) {
        meterRegistry.counter("custody.transfer.initiated",
                "chain", chain,
                "token", token
        ).increment();
    }

    /**
     * Records a chain transfer confirmation event.
     */
    public void recordTransferConfirmed(String chain, String token) {
        meterRegistry.counter("custody.transfer.confirmed",
                "chain", chain,
                "token", token
        ).increment();
    }

    /**
     * Records a chain transfer failure event.
     */
    public void recordTransferFailed(String chain, String reason) {
        meterRegistry.counter("custody.transfer.failed",
                "chain", chain,
                "reason", reason
        ).increment();
    }

    /**
     * Starts a timer sample for measuring transfer confirmation duration.
     */
    public Timer.Sample startConfirmationTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Stops the timer sample and records the transfer confirmation duration as a histogram.
     */
    public void recordConfirmationDuration(Timer.Sample sample, String chain) {
        sample.stop(meterRegistry.timer("custody.transfer.confirmation.duration", "chain", chain));
    }
}
