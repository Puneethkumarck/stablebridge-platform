package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for the fiat collection activity.
 * <p>
 * Contains the payment and banking information needed by
 * S3 Fiat On-Ramp to initiate an ACH collection via Stripe.
 */
public record FiatCollectionRequest(
        UUID paymentId,
        UUID correlationId,
        BigDecimal amount,
        String currency,
        String sourceCountry
) {}
