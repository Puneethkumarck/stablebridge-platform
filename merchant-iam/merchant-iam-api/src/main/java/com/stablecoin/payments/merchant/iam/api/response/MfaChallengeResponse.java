package com.stablecoin.payments.merchant.iam.api.response;

public record MfaChallengeResponse(
        boolean mfaRequired,
        String mfaChallengeId,
        int expiresIn
) {}
