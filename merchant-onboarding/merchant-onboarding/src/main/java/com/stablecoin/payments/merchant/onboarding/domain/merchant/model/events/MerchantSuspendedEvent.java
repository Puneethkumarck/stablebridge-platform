package com.stablecoin.payments.merchant.onboarding.domain.merchant.model.events;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record MerchantSuspendedEvent(
        String eventId,
        String eventType,
        UUID merchantId,
        String correlationId,
        String reason,
        UUID suspendedBy,
        Instant suspendedAt
) {
    public static final String TOPIC = "merchant.suspended";
    public static final String EVENT_TYPE = "merchant.suspended";
}
