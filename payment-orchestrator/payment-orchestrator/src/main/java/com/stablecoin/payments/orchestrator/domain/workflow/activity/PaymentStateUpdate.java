package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for updating the Payment aggregate state from the Temporal workflow.
 * <p>
 * Carries the terminal state (COMPLETED or FAILED) and any associated
 * metadata from the workflow result back to the database.
 */
public record PaymentStateUpdate(
        UUID paymentId,
        String terminalState,
        String failureReason,
        UUID quoteId,
        BigDecimal lockedRate,
        BigDecimal targetAmount,
        String targetCurrency,
        String chainId,
        String txHash
) {

    public static PaymentStateUpdate completed(UUID paymentId, UUID quoteId,
                                                BigDecimal lockedRate, BigDecimal targetAmount,
                                                String targetCurrency, String chainId,
                                                String txHash) {
        return new PaymentStateUpdate(paymentId, "COMPLETED", null,
                quoteId, lockedRate, targetAmount, targetCurrency, chainId, txHash);
    }

    public static PaymentStateUpdate failed(UUID paymentId, String reason) {
        return new PaymentStateUpdate(paymentId, "FAILED", reason,
                null, null, null, null, null, null);
    }
}
