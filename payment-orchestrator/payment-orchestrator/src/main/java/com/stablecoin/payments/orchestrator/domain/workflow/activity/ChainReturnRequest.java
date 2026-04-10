package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import java.math.BigDecimal;
import java.util.UUID;

public record ChainReturnRequest(
        UUID originalTransferId,
        UUID paymentId,
        String stablecoin,
        BigDecimal amount,
        String toWalletAddress,
        String reason
) {}
