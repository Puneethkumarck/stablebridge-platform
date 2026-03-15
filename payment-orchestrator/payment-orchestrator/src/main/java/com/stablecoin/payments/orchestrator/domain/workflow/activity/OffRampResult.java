package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import java.util.UUID;

/**
 * Result DTO from the off-ramp payout activity.
 * <p>
 * Contains the payout ID and initiation status. The payout settlement
 * is async — actual confirmation arrives via a partner webhook to S5,
 * which publishes a {@code fiat.payout.completed} Kafka event.
 */
public record OffRampResult(
        UUID payoutId,
        OffRampStatus status,
        String failureReason
) {

    public enum OffRampStatus {
        INITIATED,
        FAILED
    }
}
