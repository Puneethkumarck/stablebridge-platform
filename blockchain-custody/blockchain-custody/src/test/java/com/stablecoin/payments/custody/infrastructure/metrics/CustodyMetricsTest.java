package com.stablecoin.payments.custody.infrastructure.metrics;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustodyMetricsTest {

    private SimpleMeterRegistry meterRegistry;
    private CustodyMetrics custodyMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        custodyMetrics = new CustodyMetrics(meterRegistry);
    }

    @Test
    @DisplayName("should increment custody.transfer.initiated counter with chain and token tags")
    void shouldIncrementTransferInitiatedCounter() {
        custodyMetrics.recordTransferInitiated("BASE_SEPOLIA", "USDC");

        var counter = meterRegistry.find("custody.transfer.initiated")
                .tag("chain", "BASE_SEPOLIA")
                .tag("token", "USDC")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should increment custody.transfer.confirmed counter with chain and token tags")
    void shouldIncrementTransferConfirmedCounter() {
        custodyMetrics.recordTransferConfirmed("BASE_SEPOLIA", "USDC");

        var counter = meterRegistry.find("custody.transfer.confirmed")
                .tag("chain", "BASE_SEPOLIA")
                .tag("token", "USDC")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should increment custody.transfer.failed counter with chain and reason tags")
    void shouldIncrementTransferFailedCounter() {
        custodyMetrics.recordTransferFailed("BASE_SEPOLIA", "INSUFFICIENT_GAS");

        var counter = meterRegistry.find("custody.transfer.failed")
                .tag("chain", "BASE_SEPOLIA")
                .tag("reason", "INSUFFICIENT_GAS")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should record confirmation duration via timer with chain tag")
    void shouldRecordConfirmationDuration() {
        Timer.Sample sample = custodyMetrics.startConfirmationTimer();
        custodyMetrics.recordConfirmationDuration(sample, "BASE_SEPOLIA");

        var timer = meterRegistry.find("custody.transfer.confirmation.duration")
                .tag("chain", "BASE_SEPOLIA")
                .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1L);
    }
}
