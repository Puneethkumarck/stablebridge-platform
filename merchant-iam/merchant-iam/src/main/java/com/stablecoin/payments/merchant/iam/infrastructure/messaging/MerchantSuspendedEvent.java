package com.stablecoin.payments.merchant.iam.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record MerchantSuspendedEvent(
        @JsonProperty("schema_version") String schemaVersion,
        @JsonProperty("event_id")       String eventId,
        @JsonProperty("event_type")     String eventType,
        @JsonProperty("merchant_id")    UUID merchantId,
        @JsonProperty("reason")         String reason,
        @JsonProperty("suspended_at")   Instant suspendedAt
) {}
