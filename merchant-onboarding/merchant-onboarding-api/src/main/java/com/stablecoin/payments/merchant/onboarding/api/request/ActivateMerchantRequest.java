package com.stablecoin.payments.merchant.onboarding.api.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ActivateMerchantRequest(
        @NotNull UUID approvedBy,
        @NotEmpty List<String> scopes
) {}
