package com.stablecoin.payments.compliance.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Custom business metrics for the Compliance and Travel Rule service.
 *
 * <p>Tracks compliance check outcomes, sanctions hits, KYC results, and pipeline duration.
 */
@Component
@RequiredArgsConstructor
public class ComplianceMetrics {

    private final MeterRegistry meterRegistry;

    /**
     * Records a compliance check completion with its result.
     */
    public void recordCheckCompleted(String result) {
        meterRegistry.counter("compliance.check.completed",
                "result", result
        ).increment();
    }

    /**
     * Records a sanctions hit event.
     */
    public void recordSanctionsHit(String provider) {
        meterRegistry.counter("compliance.sanctions.hit",
                "provider", provider
        ).increment();
    }

    /**
     * Records a KYC verification result.
     */
    public void recordKycResult(String status) {
        meterRegistry.counter("compliance.kyc.result",
                "status", status
        ).increment();
    }

    /**
     * Starts a timer sample for measuring compliance check pipeline duration.
     */
    public Timer.Sample startCheckTimer() {
        return Timer.start(meterRegistry);
    }

    /**
     * Stops the timer sample and records the compliance check duration.
     */
    public void recordCheckDuration(Timer.Sample sample) {
        sample.stop(meterRegistry.timer("compliance.check.duration"));
    }
}
