package com.stablecoin.payments.merchant.onboarding.domain.merchant.model.events;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record MerchantKybFailedEvent(
        String eventId,
        String eventType,
        UUID merchantId,
        String correlationId,
        UUID kybId,
        String reason,
        Instant failedAt
) {
    public static final String TOPIC = "merchant.kyb.failed";
    public static final String EVENT_TYPE = "merchant.kyb.failed";
}
