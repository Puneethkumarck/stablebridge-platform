package com.stablecoin.payments.merchant.onboarding.api.request;

import jakarta.validation.constraints.NotBlank;

public record DocumentUploadRequest(
        @NotBlank String documentType,
        @NotBlank String fileName
) {}
