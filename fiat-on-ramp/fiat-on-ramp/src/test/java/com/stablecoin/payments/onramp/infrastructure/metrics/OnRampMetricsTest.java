package com.stablecoin.payments.onramp.infrastructure.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OnRampMetricsTest {

    private SimpleMeterRegistry meterRegistry;
    private OnRampMetrics onRampMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        onRampMetrics = new OnRampMetrics(meterRegistry);
    }

    @Test
    @DisplayName("should increment onramp.collection.initiated counter with psp and currency tags")
    void shouldIncrementCollectionInitiatedCounter() {
        onRampMetrics.recordCollectionInitiated("STRIPE", "USD");

        var counter = meterRegistry.find("onramp.collection.initiated")
                .tag("psp", "STRIPE")
                .tag("currency", "USD")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should increment onramp.collection.completed counter with psp and currency tags")
    void shouldIncrementCollectionCompletedCounter() {
        onRampMetrics.recordCollectionCompleted("STRIPE", "USD");

        var counter = meterRegistry.find("onramp.collection.completed")
                .tag("psp", "STRIPE")
                .tag("currency", "USD")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should increment onramp.collection.failed counter with psp and reason tags")
    void shouldIncrementCollectionFailedCounter() {
        onRampMetrics.recordCollectionFailed("STRIPE", "INSUFFICIENT_FUNDS");

        var counter = meterRegistry.find("onramp.collection.failed")
                .tag("psp", "STRIPE")
                .tag("reason", "INSUFFICIENT_FUNDS")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should increment onramp.refund.processed counter with psp tag")
    void shouldIncrementRefundProcessedCounter() {
        onRampMetrics.recordRefundProcessed("STRIPE");

        var counter = meterRegistry.find("onramp.refund.processed")
                .tag("psp", "STRIPE")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
}
