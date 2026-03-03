package com.stablecoin.payments.merchant.iam.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Inbound event produced by S11 on topic {@code merchant.closed}.
 * S13 deactivates all users and revokes all sessions.
 */
public record MerchantClosedEvent(
        @JsonProperty("schema_version") String schemaVersion,
        @JsonProperty("event_id")       String eventId,
        @JsonProperty("event_type")     String eventType,
        @JsonProperty("merchant_id")    UUID merchantId,
        @JsonProperty("closed_at")      Instant closedAt
) {}
