package com.stablecoin.payments.merchant.onboarding.api.response;

import java.time.Instant;

public record DocumentUploadResponse(
        String uploadUrl,
        Instant expiresAt
) {}
