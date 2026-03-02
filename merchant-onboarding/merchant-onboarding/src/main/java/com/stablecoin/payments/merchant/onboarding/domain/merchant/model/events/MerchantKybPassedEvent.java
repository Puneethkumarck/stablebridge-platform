package com.stablecoin.payments.merchant.onboarding.domain.merchant.model.events;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record MerchantKybPassedEvent(
        String eventId,
        String eventType,
        UUID merchantId,
        String correlationId,
        UUID kybId,
        String provider,
        String riskTier,
        Instant passedAt
) {
    public static final String TOPIC = "merchant.kyb.passed";
    public static final String EVENT_TYPE = "merchant.kyb.passed";
}
