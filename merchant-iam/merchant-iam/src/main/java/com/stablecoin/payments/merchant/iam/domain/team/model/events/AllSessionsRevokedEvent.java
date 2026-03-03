package com.stablecoin.payments.merchant.iam.domain.team.model.events;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record AllSessionsRevokedEvent(
        String schemaVersion,
        String eventId,
        String eventType,
        UUID merchantId,
        String reason,
        Instant occurredAt
) {
    public static final String TOPIC = "merchant.sessions.revoked";
    public static final String EVENT_TYPE = "merchant.sessions.revoked";
    public static final String SCHEMA_VERSION = "1.0";
}
