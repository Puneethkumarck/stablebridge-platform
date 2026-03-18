package com.stablecoin.payments.ledger.infrastructure.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LedgerMetricsTest {

    private SimpleMeterRegistry meterRegistry;
    private LedgerMetrics ledgerMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        ledgerMetrics = new LedgerMetrics(meterRegistry);
    }

    @Test
    @DisplayName("should increment ledger.transaction.posted counter with type and currency tags")
    void shouldIncrementTransactionPostedCounter() {
        ledgerMetrics.recordTransactionPosted("PAYMENT", "USD");

        var counter = meterRegistry.find("ledger.transaction.posted")
                .tag("type", "PAYMENT")
                .tag("currency", "USD")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should increment ledger.reconciliation.completed counter with MATCHED status")
    void shouldIncrementReconciliationCompletedCounterForMatched() {
        ledgerMetrics.recordReconciliationCompleted("MATCHED");

        var counter = meterRegistry.find("ledger.reconciliation.completed")
                .tag("status", "MATCHED")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should increment ledger.reconciliation.completed counter with DISCREPANCY status")
    void shouldIncrementReconciliationCompletedCounterForDiscrepancy() {
        ledgerMetrics.recordReconciliationCompleted("DISCREPANCY");

        var counter = meterRegistry.find("ledger.reconciliation.completed")
                .tag("status", "DISCREPANCY")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should increment ledger.reconciliation.discrepancy counter")
    void shouldIncrementReconciliationDiscrepancyCounter() {
        ledgerMetrics.recordReconciliationDiscrepancy();

        var counter = meterRegistry.find("ledger.reconciliation.discrepancy").counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should accumulate counts across multiple increments")
    void shouldAccumulateCountsAcrossMultipleIncrements() {
        ledgerMetrics.recordTransactionPosted("PAYMENT", "USD");
        ledgerMetrics.recordTransactionPosted("PAYMENT", "USD");

        var counter = meterRegistry.find("ledger.transaction.posted")
                .tag("type", "PAYMENT")
                .tag("currency", "USD")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);
    }
}
