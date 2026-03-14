package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for the chain transfer activity.
 * <p>
 * Contains the payment and stablecoin information needed by
 * S4 Blockchain &amp; Custody to submit a USDC transfer on-chain.
 */
public record ChainTransferRequest(
        UUID paymentId,
        UUID correlationId,
        String stablecoin,
        BigDecimal amount,
        String toWalletAddress,
        String preferredChain
) {}
