package com.stablecoin.payments.merchant.iam.api.response;

public record PermissionCheckResponse(
        boolean allowed,
        String role,
        String via
) {}
