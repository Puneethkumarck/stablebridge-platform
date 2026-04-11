package com.stablecoin.payments.merchant.iam.api.response;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID userId,
        UUID merchantId,
        String email,
        String fullName,
        RoleSummary role,
        String status,
        boolean mfaEnabled,
        Instant lastLoginAt,
        Instant activatedAt,
        Instant suspendedAt,
        Instant createdAt
) {}
