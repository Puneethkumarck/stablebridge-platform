package com.stablecoin.payments.merchant.iam.api.response;

import java.time.Instant;
import java.util.UUID;

public record ChangeRoleResponse(
        UUID userId,
        String oldRole,
        String newRole,
        Instant changedAt,
        UUID changedBy
) {}
