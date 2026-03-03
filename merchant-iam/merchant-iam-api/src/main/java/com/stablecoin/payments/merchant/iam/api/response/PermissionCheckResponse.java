package com.stablecoin.payments.merchant.iam.api.response;

/**
 * Response for {@code GET /v1/auth/permissions/check} — used by S10 API Gateway.
 */
public record PermissionCheckResponse(
        boolean allowed,
        String role,
        String via
) {}
