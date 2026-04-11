package com.stablecoin.payments.merchant.iam.api.response;

import java.time.Instant;
import java.util.UUID;

public record ReactivateUserResponse(UUID userId, String status, Instant activatedAt) {}
