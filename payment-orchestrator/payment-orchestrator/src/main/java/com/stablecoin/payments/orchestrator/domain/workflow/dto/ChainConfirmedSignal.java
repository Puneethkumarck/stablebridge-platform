package com.stablecoin.payments.orchestrator.domain.workflow.dto;

import java.util.UUID;

public record ChainConfirmedSignal(
        UUID paymentId,
        String txHash,
        String chainId,
        long blockNumber
) {}
