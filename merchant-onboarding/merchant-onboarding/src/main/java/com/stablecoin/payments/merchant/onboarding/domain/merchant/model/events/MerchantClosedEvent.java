package com.stablecoin.payments.merchant.onboarding.domain.merchant.model.events;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record MerchantClosedEvent(
        String eventId,
        String eventType,
        UUID merchantId,
        String correlationId,
        String reason,
        UUID closedBy,
        Instant closedAt
) {
    public static final String TOPIC = "merchant.closed";
    public static final String EVENT_TYPE = "merchant.closed";
}
