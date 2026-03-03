package com.stablecoin.payments.merchant.iam.api.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Returned by {@code POST /v1/merchants/{merchantId}/users/{userId}/suspend}.
 */
public record SuspendUserResponse(UUID userId, String status, Instant suspendedAt) {}
