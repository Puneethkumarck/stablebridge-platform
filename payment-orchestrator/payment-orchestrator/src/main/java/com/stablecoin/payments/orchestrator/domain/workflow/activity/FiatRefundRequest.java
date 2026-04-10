package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import java.math.BigDecimal;
import java.util.UUID;

public record FiatRefundRequest(
        UUID collectionId,
        UUID paymentId,
        BigDecimal refundAmount,
        String currency,
        String reason
) {}
