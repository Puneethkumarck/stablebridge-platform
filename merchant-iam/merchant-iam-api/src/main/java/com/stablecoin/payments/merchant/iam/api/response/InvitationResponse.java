package com.stablecoin.payments.merchant.iam.api.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Returned by {@code POST /v1/merchants/{merchantId}/users/invite}.
 */
public record InvitationResponse(
        UUID invitationId,
        String email,
        String role,
        String status,
        Instant expiresAt,
        UUID invitedBy,
        Instant createdAt
) {}
