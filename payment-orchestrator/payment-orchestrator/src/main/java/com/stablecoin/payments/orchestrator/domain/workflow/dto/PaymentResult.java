package com.stablecoin.payments.orchestrator.domain.workflow.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResult(
        UUID paymentId,
        PaymentResultStatus status,
        String failureReason,
        UUID quoteId,
        BigDecimal lockedRate,
        BigDecimal targetAmount,
        String targetCurrency
) {

    public enum PaymentResultStatus {
        COMPLETED,
        FAILED
    }

    public static PaymentResult completed(UUID paymentId, UUID quoteId,
                                          BigDecimal lockedRate, BigDecimal targetAmount,
                                          String targetCurrency) {
        return new PaymentResult(paymentId, PaymentResultStatus.COMPLETED, null,
                quoteId, lockedRate, targetAmount, targetCurrency);
    }

    public static PaymentResult failed(UUID paymentId, String reason) {
        return new PaymentResult(paymentId, PaymentResultStatus.FAILED, reason,
                null, null, null, null);
    }
}
