package com.stablecoin.payments.merchant.iam.api.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Returned by {@code PATCH /v1/merchants/{merchantId}/users/{userId}/role}.
 */
public record ChangeRoleResponse(
        UUID userId,
        String oldRole,
        String newRole,
        Instant changedAt,
        UUID changedBy
) {}
