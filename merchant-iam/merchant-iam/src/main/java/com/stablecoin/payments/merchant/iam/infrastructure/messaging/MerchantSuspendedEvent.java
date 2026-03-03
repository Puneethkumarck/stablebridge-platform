package com.stablecoin.payments.merchant.iam.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Inbound event produced by S11 on topic {@code merchant.suspended}.
 * S13 revokes all user sessions for the merchant.
 */
public record MerchantSuspendedEvent(
        @JsonProperty("schema_version") String schemaVersion,
        @JsonProperty("event_id")       String eventId,
        @JsonProperty("event_type")     String eventType,
        @JsonProperty("merchant_id")    UUID merchantId,
        @JsonProperty("reason")         String reason,
        @JsonProperty("suspended_at")   Instant suspendedAt
) {}
