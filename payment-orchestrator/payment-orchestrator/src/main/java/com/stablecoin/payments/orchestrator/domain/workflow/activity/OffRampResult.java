package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import java.util.UUID;

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
