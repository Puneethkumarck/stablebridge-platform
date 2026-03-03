package com.stablecoin.payments.merchant.iam.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InviteUserRequest(
        @NotBlank @Email String email,
        @NotBlank String fullName,
        @NotNull UUID roleId
) {}
