package com.stablecoin.payments.orchestrator.domain.workflow.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record FiatCollectedSignal(
        UUID paymentId,
        String providerReference,
        BigDecimal collectedAmount,
        String currency
) {}
