package com.stablecoin.payments.merchant.iam.api.response;

import java.util.List;
import java.util.UUID;

/**
 * Returned on successful login (no MFA, or after MFA verify).
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        int expiresIn,
        UserInfo user
) {

    public record UserInfo(
            UUID userId,
            UUID merchantId,
            String fullName,
            String role,
            List<String> permissions
    ) {}
}
