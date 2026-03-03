package com.stablecoin.payments.merchant.iam.api.response;

public record RefreshTokenResponse(String accessToken, int expiresIn) {}
