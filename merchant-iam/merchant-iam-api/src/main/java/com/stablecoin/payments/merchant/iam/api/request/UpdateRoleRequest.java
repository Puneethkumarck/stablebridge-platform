package com.stablecoin.payments.merchant.iam.api.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateRoleRequest(
        @NotEmpty List<@NotEmpty String> permissions
) {}
