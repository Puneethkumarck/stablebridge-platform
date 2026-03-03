package com.stablecoin.payments.merchant.iam.domain.team.model.events;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record MerchantUserInvitedEvent(
        String schemaVersion,
        String eventId,
        String eventType,
        UUID invitationId,
        UUID userId,
        UUID merchantId,
        String emailHash,
        UUID roleId,
        String roleName,
        UUID invitedBy,
        Instant occurredAt
) {
    public static final String TOPIC = "merchant.user.invited";
    public static final String EVENT_TYPE = "merchant.user.invited";
    public static final String SCHEMA_VERSION = "1.0";
}
