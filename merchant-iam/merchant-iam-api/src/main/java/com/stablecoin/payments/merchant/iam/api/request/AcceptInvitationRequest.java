package com.stablecoin.payments.merchant.iam.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptInvitationRequest(
        @NotBlank String fullName,
        @NotBlank @Size(min = 12, message = "Password must be at least 12 characters") String password
) {}
