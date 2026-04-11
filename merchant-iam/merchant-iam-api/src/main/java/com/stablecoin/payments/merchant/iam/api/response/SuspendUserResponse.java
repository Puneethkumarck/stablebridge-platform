package com.stablecoin.payments.merchant.iam.api.response;

import java.time.Instant;
import java.util.UUID;

public record SuspendUserResponse(UUID userId, String status, Instant suspendedAt) {}
