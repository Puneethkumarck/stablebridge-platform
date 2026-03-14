package com.stablecoin.payments.orchestrator.domain.workflow.activity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for the chain transfer return compensation activity.
 * <p>
 * Used during saga compensation to submit a return transfer of stablecoin
 * back to the originating wallet via S4 Blockchain &amp; Custody.
 */
public record ChainReturnRequest(
        UUID originalTransferId,
        UUID paymentId,
        String stablecoin,
        BigDecimal amount,
        String toWalletAddress,
        String reason
) {}
