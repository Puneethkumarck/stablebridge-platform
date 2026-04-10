package com.stablecoin.payments.ledger.domain.event;

import com.stablecoin.payments.ledger.domain.model.ReconciliationStatus;

import java.time.Instant;
import java.util.UUID;

public record ReconciliationCompletedDomainEvent(
        UUID recId,
        UUID paymentId,
        ReconciliationStatus status,
        Instant completedAt
) {
}
