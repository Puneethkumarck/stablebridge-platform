package com.stablecoin.payments.merchant.onboarding.domain.merchant.model.events;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record MerchantCorridorApprovedEvent(
        String eventId,
        String eventType,
        UUID merchantId,
        String correlationId,
        UUID corridorId,
        String sourceCountry,
        String targetCountry,
        String maxAmountUsd,
        Instant approvedAt
) {
    public static final String TOPIC = "merchant.corridor.approved";
    public static final String EVENT_TYPE = "merchant.corridor.approved";
}
