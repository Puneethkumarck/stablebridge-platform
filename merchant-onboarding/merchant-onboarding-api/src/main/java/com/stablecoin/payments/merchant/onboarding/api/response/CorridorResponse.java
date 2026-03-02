package com.stablecoin.payments.merchant.onboarding.api.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CorridorResponse(
        UUID corridorId,
        UUID merchantId,
        String sourceCountry,
        String targetCountry,
        List<String> currencies,
        BigDecimal maxAmountUsd,
        UUID approvedBy,
        Instant approvedAt,
        Instant expiresAt,
        boolean active
) {}
