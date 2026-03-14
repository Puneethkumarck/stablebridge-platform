package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import java.util.UUID;

/**
 * Result DTO from the chain transfer activity.
 * <p>
 * Contains the transfer ID and submission status. The transfer confirmation
 * is async — arrives via the {@code onChainConfirmed} workflow signal after
 * S4's block monitor detects sufficient confirmations.
 */
public record ChainTransferResult(
        UUID transferId,
        String chainId,
        String txHash,
        ChainTransferStatus status,
        String failureReason
) {

    public enum ChainTransferStatus {
        SUBMITTED,
        FAILED
    }
}
