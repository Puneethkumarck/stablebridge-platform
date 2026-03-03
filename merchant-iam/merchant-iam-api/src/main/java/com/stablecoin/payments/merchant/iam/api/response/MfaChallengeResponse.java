package com.stablecoin.payments.merchant.iam.api.response;

/**
 * Returned on login when MFA is required — client must call {@code POST /v1/auth/mfa/verify}.
 */
public record MfaChallengeResponse(
        boolean mfaRequired,
        String mfaChallengeId,
        int expiresIn
) {}
