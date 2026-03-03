package com.stablecoin.payments.merchant.iam.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MfaVerifyRequest(
        @NotBlank String mfaChallengeId,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "TOTP code must be 6 digits") String totpCode
) {}
