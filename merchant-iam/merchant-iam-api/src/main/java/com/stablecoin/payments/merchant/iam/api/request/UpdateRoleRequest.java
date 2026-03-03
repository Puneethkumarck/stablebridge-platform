package com.stablecoin.payments.merchant.iam.api.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Sent to {@code PATCH /v1/merchants/{merchantId}/roles/{roleId}}.
 * Only permissions can be updated on a custom role.
 */
public record UpdateRoleRequest(
        @NotEmpty List<@NotEmpty String> permissions
) {}
