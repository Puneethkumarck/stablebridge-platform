package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for the off-ramp payout activity.
 * <p>
 * Contains the payment, transfer, and recipient information needed by
 * S5 Fiat Off-Ramp to initiate a SEPA payout via Modulr.
 */
public record OffRampRequest(
        UUID paymentId,
        UUID correlationId,
        UUID transferId,
        String stablecoin,
        BigDecimal redeemedAmount,
        String targetCurrency,
        BigDecimal appliedFxRate,
        UUID recipientId
) {}
