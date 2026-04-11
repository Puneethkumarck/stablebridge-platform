package com.stablecoin.payments.custody.domain.model;

import lombok.Builder;

@Builder(toBuilder = true)
public record ChainCandidate(
        ChainId chainId,
        double feeUsd,
        int finalitySeconds,
        double healthScore,
        double score,
        boolean selected
) {}
