package com.stablecoin.payments.merchant.iam.api.request;

import jakarta.validation.constraints.NotBlank;

public record DeactivateUserRequest(@NotBlank String reason) {}
