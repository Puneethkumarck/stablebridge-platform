package com.stablecoin.payments.ledger.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LedgerMetrics {

    private final MeterRegistry meterRegistry;

    public void recordTransactionPosted(String type, String currency) {
        meterRegistry.counter("ledger.transaction.posted",
                "type", type,
                "currency", currency
        ).increment();
    }

    public void recordReconciliationCompleted(String status) {
        meterRegistry.counter("ledger.reconciliation.completed",
                "status", status
        ).increment();
    }

    public void recordReconciliationDiscrepancy() {
        meterRegistry.counter("ledger.reconciliation.discrepancy").increment();
    }
}
