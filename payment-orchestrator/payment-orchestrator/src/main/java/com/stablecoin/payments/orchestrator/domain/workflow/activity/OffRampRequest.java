package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import java.math.BigDecimal;
import java.util.UUID;

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
