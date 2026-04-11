package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import java.math.BigDecimal;
import java.util.UUID;

public record ChainTransferRequest(
        UUID paymentId,
        UUID correlationId,
        String stablecoin,
        BigDecimal amount,
        String toWalletAddress,
        String preferredChain
) {}
