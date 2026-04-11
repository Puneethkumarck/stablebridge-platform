package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import java.util.UUID;

public record FiatCollectionResult(
        UUID collectionId,
        FiatCollectionStatus status,
        String failureReason
) {

    public enum FiatCollectionStatus {
        INITIATED,
        FAILED
    }
}
