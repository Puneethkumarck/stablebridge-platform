package com.stablecoin.payments.merchant.onboarding.api.response;

import java.time.Instant;
import java.util.UUID;

public record MerchantApplicationResponse(
        UUID merchantId,
        String status,
        String kybStatus,
        String legalName,
        Instant createdAt
) {}
