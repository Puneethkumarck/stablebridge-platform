package com.stablecoin.payments.merchant.iam.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateRoleRequest(
        @NotBlank @Size(min = 2, max = 50) String roleName,
        @NotBlank @Size(max = 255) String description,
        @NotEmpty List<@NotBlank String> permissions
) {}
