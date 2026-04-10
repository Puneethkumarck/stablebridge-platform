package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import java.math.BigDecimal;
import java.util.UUID;

public record FiatCollectionRequest(
        UUID paymentId,
        UUID correlationId,
        BigDecimal amount,
        String currency,
        String sourceCountry
) {}
