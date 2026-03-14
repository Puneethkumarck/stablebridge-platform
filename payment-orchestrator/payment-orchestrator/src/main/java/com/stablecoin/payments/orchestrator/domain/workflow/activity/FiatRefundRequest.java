package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for the fiat collection refund compensation activity.
 * <p>
 * Used during saga compensation to refund a previously collected fiat payment
 * back to the sender via S3 Fiat On-Ramp.
 */
public record FiatRefundRequest(
        UUID collectionId,
        UUID paymentId,
        BigDecimal refundAmount,
        String currency,
        String reason
) {}
