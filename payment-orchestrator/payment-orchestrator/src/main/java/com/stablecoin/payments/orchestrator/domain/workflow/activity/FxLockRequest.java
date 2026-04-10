package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import java.math.BigDecimal;
import java.util.UUID;

public record FxLockRequest(
        String idempotencyKey,
        UUID paymentId,
        String sourceCurrency,
        String targetCurrency,
        BigDecimal sourceAmount,
        String sourceCountry,
        String targetCountry
) {}
