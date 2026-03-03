package com.stablecoin.payments.merchant.iam.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Inbound event produced by S11 Merchant Onboarding on topic {@code merchant.activated}.
 * S13 uses this to seed built-in roles and create the first ADMIN user.
 */
public record MerchantActivatedEvent(
        @JsonProperty("schema_version")  String schemaVersion,
        @JsonProperty("event_id")        String eventId,
        @JsonProperty("event_type")      String eventType,
        @JsonProperty("merchant_id")     UUID merchantId,
        @JsonProperty("company_name")    String companyName,
        @JsonProperty("primary_contact_email") String primaryContactEmail,
        @JsonProperty("primary_contact_name")  String primaryContactName,
        @JsonProperty("activated_at")    Instant activatedAt
) {}
