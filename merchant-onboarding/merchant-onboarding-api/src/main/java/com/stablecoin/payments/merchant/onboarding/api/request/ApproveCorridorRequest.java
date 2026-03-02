package com.stablecoin.payments.merchant.onboarding.api.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ApproveCorridorRequest(
        @NotBlank @Size(min = 2, max = 2) String sourceCountry,
        @NotBlank @Size(min = 2, max = 2) String targetCountry,
        @NotEmpty List<String> currencies,
        @NotNull @DecimalMin("1.00") BigDecimal maxAmountUsd,
        @NotNull Instant expiresAt
) {}
