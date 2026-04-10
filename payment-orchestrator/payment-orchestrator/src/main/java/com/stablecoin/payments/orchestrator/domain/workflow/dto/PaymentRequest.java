package com.stablecoin.payments.orchestrator.domain.workflow.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRequest(
        UUID paymentId,
        String idempotencyKey,
        UUID correlationId,
        UUID senderId,
        UUID recipientId,
        BigDecimal sourceAmount,
        String sourceCurrency,
        String targetCurrency,
        String sourceCountry,
        String targetCountry
) {}
