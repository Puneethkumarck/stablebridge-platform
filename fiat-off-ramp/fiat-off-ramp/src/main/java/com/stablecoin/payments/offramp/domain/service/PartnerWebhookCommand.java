package com.stablecoin.payments.offramp.domain.service;

import java.math.BigDecimal;
import java.time.Instant;

public record PartnerWebhookCommand(
        String eventId,
        String eventType,
        String partnerName,
        String partnerReference,
        BigDecimal amount,
        String currency,
        String status,
        Instant settledAt,
        String failureReason,
        String rawPayload
) {

    public static final String EVENT_PAYMENT_SETTLED = "payment.settled";
    public static final String EVENT_PAYMENT_FAILED = "payment.failed";

    public PartnerWebhookCommand {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId is required");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventType is required");
        }
        if (partnerName == null || partnerName.isBlank()) {
            throw new IllegalArgumentException("partnerName is required");
        }
        if (partnerReference == null || partnerReference.isBlank()) {
            throw new IllegalArgumentException("partnerReference is required");
        }
    }
}
