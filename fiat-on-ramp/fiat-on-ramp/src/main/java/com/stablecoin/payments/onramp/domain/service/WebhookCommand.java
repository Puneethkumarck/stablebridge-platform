package com.stablecoin.payments.onramp.domain.service;

import com.stablecoin.payments.onramp.domain.model.Money;

import java.util.UUID;

public record WebhookCommand(
        String eventId,
        String eventType,
        String pspReference,
        UUID collectionId,
        Money amount,
        String status,
        String rawPayload
) {

    public WebhookCommand {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("eventId is required");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("eventType is required");
        }
        if (pspReference == null || pspReference.isBlank()) {
            throw new IllegalArgumentException("pspReference is required");
        }
    }
}
