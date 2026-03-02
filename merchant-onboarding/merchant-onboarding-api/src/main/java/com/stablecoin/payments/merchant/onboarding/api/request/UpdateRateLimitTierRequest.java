package com.stablecoin.payments.merchant.onboarding.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateRateLimitTierRequest(
        @NotBlank String newTier,
        @NotNull UUID updatedBy
) {}
