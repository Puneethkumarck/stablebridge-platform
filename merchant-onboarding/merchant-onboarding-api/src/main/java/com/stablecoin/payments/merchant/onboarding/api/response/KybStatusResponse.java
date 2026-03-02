package com.stablecoin.payments.merchant.onboarding.api.response;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record KybStatusResponse(
        UUID kybId,
        String status,
        String provider,
        String providerRef,
        Instant initiatedAt,
        Instant completedAt,
        Map<String, Object> riskSignals
) {}
