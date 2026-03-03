package com.stablecoin.payments.merchant.iam.api.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full role representation — used in list and create/update responses.
 */
public record RoleResponse(
        UUID roleId,
        String roleName,
        String description,
        boolean builtin,
        boolean active,
        long userCount,
        List<String> permissions,
        Instant createdAt,
        Instant updatedAt
) {}
