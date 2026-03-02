package com.stablecoin.payments.merchant.onboarding.api.request;

import java.util.UUID;

public record CloseMerchantRequest(
        String reason,
        UUID closedBy
) {}
