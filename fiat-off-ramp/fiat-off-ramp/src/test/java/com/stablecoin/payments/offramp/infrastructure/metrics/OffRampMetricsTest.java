package com.stablecoin.payments.offramp.infrastructure.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OffRampMetricsTest {

    private SimpleMeterRegistry meterRegistry;
    private OffRampMetrics offRampMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        offRampMetrics = new OffRampMetrics(meterRegistry);
    }

    @Test
    @DisplayName("should increment offramp.payout.initiated counter with partner and currency tags")
    void shouldIncrementPayoutInitiatedCounter() {
        offRampMetrics.recordPayoutInitiated("MODULR", "EUR");

        var counter = meterRegistry.find("offramp.payout.initiated")
                .tag("partner", "MODULR")
                .tag("currency", "EUR")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should increment offramp.payout.completed counter with partner and currency tags")
    void shouldIncrementPayoutCompletedCounter() {
        offRampMetrics.recordPayoutCompleted("MODULR", "EUR");

        var counter = meterRegistry.find("offramp.payout.completed")
                .tag("partner", "MODULR")
                .tag("currency", "EUR")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should increment offramp.payout.failed counter with partner and reason tags")
    void shouldIncrementPayoutFailedCounter() {
        offRampMetrics.recordPayoutFailed("MODULR", "ACCOUNT_CLOSED");

        var counter = meterRegistry.find("offramp.payout.failed")
                .tag("partner", "MODULR")
                .tag("reason", "ACCOUNT_CLOSED")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should increment offramp.redemption.completed counter with provider tag")
    void shouldIncrementRedemptionCompletedCounter() {
        offRampMetrics.recordRedemptionCompleted("CIRCLE");

        var counter = meterRegistry.find("offramp.redemption.completed")
                .tag("provider", "CIRCLE")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
}
