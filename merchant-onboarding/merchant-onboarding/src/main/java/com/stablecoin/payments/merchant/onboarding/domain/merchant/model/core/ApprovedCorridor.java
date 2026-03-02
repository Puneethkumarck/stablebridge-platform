package com.stablecoin.payments.merchant.onboarding.domain.merchant.model.core;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder(toBuilder = true)
public record ApprovedCorridor(
        UUID corridorId,
        UUID merchantId,
        String sourceCountry,
        String targetCountry,
        List<String> currencies,
        BigDecimal maxAmountUsd,
        UUID approvedBy,
        Instant approvedAt,
        Instant expiresAt,
        boolean isActive
) {}
