package com.stablecoin.payments.merchant.onboarding.api.request;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record SuspendMerchantRequest(
        @NotBlank String reason,
        UUID suspendedBy
) {}
