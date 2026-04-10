package com.stablecoin.payments.compliance.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ComplianceMetrics {

    private final MeterRegistry meterRegistry;

    public void recordCheckCompleted(String result) {
        meterRegistry.counter("compliance.check.completed",
                "result", result
        ).increment();
    }

    public void recordSanctionsHit(String provider) {
        meterRegistry.counter("compliance.sanctions.hit",
                "provider", provider
        ).increment();
    }

    public void recordKycResult(String status) {
        meterRegistry.counter("compliance.kyc.result",
                "status", status
        ).increment();
    }

    public Timer.Sample startCheckTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordCheckDuration(Timer.Sample sample) {
        sample.stop(meterRegistry.timer("compliance.check.duration"));
    }
}
