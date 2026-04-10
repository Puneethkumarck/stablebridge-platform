package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import java.math.BigDecimal;
import java.util.UUID;

public record FxLockResult(
        UUID lockId,
        UUID quoteId,
        BigDecimal lockedRate,
        BigDecimal targetAmount,
        String targetCurrency,
        FxLockStatus status,
        String failureReason
) {

    public enum FxLockStatus {
        LOCKED,
        FAILED,
        INSUFFICIENT_LIQUIDITY
    }

    // Note: convenience methods like isLocked() are intentionally omitted
    // to avoid Jackson serialization issues with Temporal SDK's internal serializer.
    // Use status() == FxLockStatus.LOCKED directly in workflow code.
}
