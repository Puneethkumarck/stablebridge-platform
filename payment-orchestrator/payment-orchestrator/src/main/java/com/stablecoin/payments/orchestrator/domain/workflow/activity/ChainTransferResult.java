package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import java.util.UUID;

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
