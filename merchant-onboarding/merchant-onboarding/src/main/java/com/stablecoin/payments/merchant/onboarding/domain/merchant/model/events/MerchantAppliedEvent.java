package com.stablecoin.payments.merchant.onboarding.domain.merchant.model.events;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record MerchantAppliedEvent(
        String eventId,
        String eventType,
        UUID merchantId,
        String correlationId,
        String legalName,
        String registrationCountry,
        String entityType,
        Instant appliedAt
) {
    public static final String TOPIC = "merchant.applied";
    public static final String EVENT_TYPE = "merchant.applied";
}
