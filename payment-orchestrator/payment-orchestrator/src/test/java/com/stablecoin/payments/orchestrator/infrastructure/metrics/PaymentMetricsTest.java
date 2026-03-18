package com.stablecoin.payments.orchestrator.infrastructure.metrics;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMetricsTest {

    private SimpleMeterRegistry meterRegistry;
    private PaymentMetrics paymentMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        paymentMetrics = new PaymentMetrics(meterRegistry);
    }

    @Test
    @DisplayName("should increment payment.initiated counter with corridor and currency tags")
    void shouldIncrementPaymentInitiatedCounter() {
        paymentMetrics.recordPaymentInitiated("US_DE", "USD");

        var counter = meterRegistry.find("payment.initiated")
                .tag("corridor", "US_DE")
                .tag("currency", "USD")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should increment payment.completed counter with corridor and currency tags")
    void shouldIncrementPaymentCompletedCounter() {
        paymentMetrics.recordPaymentCompleted("US_DE", "EUR");

        var counter = meterRegistry.find("payment.completed")
                .tag("corridor", "US_DE")
                .tag("currency", "EUR")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should increment payment.failed counter with corridor and reason tags")
    void shouldIncrementPaymentFailedCounter() {
        paymentMetrics.recordPaymentFailed("US_DE", "COMPLIANCE_REJECTED");

        var counter = meterRegistry.find("payment.failed")
                .tag("corridor", "US_DE")
                .tag("reason", "COMPLIANCE_REJECTED")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should record payment duration via timer")
    void shouldRecordPaymentDuration() {
        Timer.Sample sample = paymentMetrics.startPaymentTimer();
        paymentMetrics.recordPaymentDuration(sample, "US_DE");

        var timer = meterRegistry.find("payment.duration")
                .tag("corridor", "US_DE")
                .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("should accumulate counts across multiple increments")
    void shouldAccumulateCountsAcrossMultipleIncrements() {
        paymentMetrics.recordPaymentInitiated("US_DE", "USD");
        paymentMetrics.recordPaymentInitiated("US_DE", "USD");
        paymentMetrics.recordPaymentInitiated("US_DE", "USD");

        var counter = meterRegistry.find("payment.initiated")
                .tag("corridor", "US_DE")
                .tag("currency", "USD")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(3.0);
    }
}
