package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import java.util.UUID;

/**
 * Result DTO from the fiat collection activity.
 * <p>
 * Contains the collection order ID and initiation status. The collection
 * itself is async — actual confirmation arrives via the {@code onFiatCollected}
 * workflow signal after a Stripe webhook is received by S3.
 */
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
