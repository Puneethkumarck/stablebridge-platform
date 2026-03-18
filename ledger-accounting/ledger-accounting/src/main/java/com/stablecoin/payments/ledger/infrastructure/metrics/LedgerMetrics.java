package com.stablecoin.payments.ledger.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Custom business metrics for the Ledger and Accounting service.
 *
 * <p>Tracks transaction posting, reconciliation completion, and discrepancy detection.
 */
@Component
@RequiredArgsConstructor
public class LedgerMetrics {

    private final MeterRegistry meterRegistry;

    /**
     * Records a ledger transaction posted event.
     */
    public void recordTransactionPosted(String type, String currency) {
        meterRegistry.counter("ledger.transaction.posted",
                "type", type,
                "currency", currency
        ).increment();
    }

    /**
     * Records a reconciliation completion event.
     */
    public void recordReconciliationCompleted(String status) {
        meterRegistry.counter("ledger.reconciliation.completed",
                "status", status
        ).increment();
    }

    /**
     * Records a reconciliation discrepancy event.
     */
    public void recordReconciliationDiscrepancy() {
        meterRegistry.counter("ledger.reconciliation.discrepancy").increment();
    }
}
