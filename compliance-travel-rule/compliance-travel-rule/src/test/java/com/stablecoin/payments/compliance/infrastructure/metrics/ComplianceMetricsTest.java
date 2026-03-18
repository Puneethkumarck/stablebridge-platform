package com.stablecoin.payments.compliance.infrastructure.metrics;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ComplianceMetricsTest {

    private SimpleMeterRegistry meterRegistry;
    private ComplianceMetrics complianceMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        complianceMetrics = new ComplianceMetrics(meterRegistry);
    }

    @Test
    @DisplayName("should increment compliance.check.completed counter with result tag")
    void shouldIncrementCheckCompletedCounter() {
        complianceMetrics.recordCheckCompleted("APPROVED");

        var counter = meterRegistry.find("compliance.check.completed")
                .tag("result", "APPROVED")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should increment compliance.check.completed counter for REJECTED result")
    void shouldIncrementCheckCompletedCounterForRejected() {
        complianceMetrics.recordCheckCompleted("REJECTED");

        var counter = meterRegistry.find("compliance.check.completed")
                .tag("result", "REJECTED")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should increment compliance.sanctions.hit counter with provider tag")
    void shouldIncrementSanctionsHitCounter() {
        complianceMetrics.recordSanctionsHit("WORLD_CHECK");

        var counter = meterRegistry.find("compliance.sanctions.hit")
                .tag("provider", "WORLD_CHECK")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should increment compliance.kyc.result counter with status tag")
    void shouldIncrementKycResultCounter() {
        complianceMetrics.recordKycResult("VERIFIED");

        var counter = meterRegistry.find("compliance.kyc.result")
                .tag("status", "VERIFIED")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should record compliance check duration via timer")
    void shouldRecordCheckDuration() {
        Timer.Sample sample = complianceMetrics.startCheckTimer();
        complianceMetrics.recordCheckDuration(sample);

        var timer = meterRegistry.find("compliance.check.duration").timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1L);
    }
}
