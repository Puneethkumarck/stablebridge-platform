package com.stablecoin.payments.ledger.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReconciliationDiscrepancyDomainEvent(
        UUID recId,
        UUID paymentId,
        BigDecimal discrepancy,
        String currency,
        String detail,
        Instant detectedAt
) {
}
