package com.stablecoin.payments.fx.infrastructure.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FxMetricsTest {

    private SimpleMeterRegistry meterRegistry;
    private FxMetrics fxMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        fxMetrics = new FxMetrics(meterRegistry);
    }

    @Test
    @DisplayName("should increment fx.quote.created counter with corridor tag")
    void shouldIncrementQuoteCreatedCounter() {
        fxMetrics.recordQuoteCreated("USD_EUR");

        var counter = meterRegistry.find("fx.quote.created")
                .tag("corridor", "USD_EUR")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should increment fx.lock.acquired counter with corridor tag")
    void shouldIncrementLockAcquiredCounter() {
        fxMetrics.recordLockAcquired("USD_EUR");

        var counter = meterRegistry.find("fx.lock.acquired")
                .tag("corridor", "USD_EUR")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should increment fx.lock.expired counter with corridor tag")
    void shouldIncrementLockExpiredCounter() {
        fxMetrics.recordLockExpired("USD_EUR");

        var counter = meterRegistry.find("fx.lock.expired")
                .tag("corridor", "USD_EUR")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should track active lock count gauge for corridor")
    void shouldTrackActiveLockCountGauge() {
        fxMetrics.incrementActiveLocks("USD_EUR");
        fxMetrics.incrementActiveLocks("USD_EUR");

        var gauge = meterRegistry.find("fx.lock.active.count")
                .tag("corridor", "USD_EUR")
                .gauge();

        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("should decrement active lock count gauge for corridor")
    void shouldDecrementActiveLockCountGauge() {
        fxMetrics.incrementActiveLocks("USD_EUR");
        fxMetrics.incrementActiveLocks("USD_EUR");
        fxMetrics.decrementActiveLocks("USD_EUR");

        var gauge = meterRegistry.find("fx.lock.active.count")
                .tag("corridor", "USD_EUR")
                .gauge();

        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should track independent gauges for different corridors")
    void shouldTrackIndependentGaugesForDifferentCorridors() {
        fxMetrics.incrementActiveLocks("USD_EUR");
        fxMetrics.incrementActiveLocks("USD_EUR");
        fxMetrics.incrementActiveLocks("GBP_EUR");

        var usdEurGauge = meterRegistry.find("fx.lock.active.count")
                .tag("corridor", "USD_EUR")
                .gauge();
        var gbpEurGauge = meterRegistry.find("fx.lock.active.count")
                .tag("corridor", "GBP_EUR")
                .gauge();

        assertThat(usdEurGauge).isNotNull();
        assertThat(usdEurGauge.value()).isEqualTo(2.0);
        assertThat(gbpEurGauge).isNotNull();
        assertThat(gbpEurGauge.value()).isEqualTo(1.0);
    }
}
