package com.stablecoin.payments.merchant.iam.domain.team;

import com.stablecoin.payments.merchant.iam.domain.team.model.MerchantUser;
import com.stablecoin.payments.merchant.iam.domain.team.model.Role;

import java.util.List;
import java.util.UUID;

public interface JwtTokenIssuer {

    String issueAccessToken(MerchantUser user, Role role, boolean mfaVerified);

    String issueRefreshToken(UUID userId, UUID sessionId);

    ParsedAccessToken parseAndVerify(String token);

    ParsedRefreshToken parseRefreshToken(String token);

    int refreshTokenTtlSeconds();

    String jwksJson();

    record ParsedRefreshToken(
            UUID jti,
            UUID userId,
            UUID sessionId,
            long expiresAtEpochSecond
    ) {}

    record ParsedAccessToken(
            UUID jti,
            UUID userId,
            UUID merchantId,
            UUID roleId,
            String role,
            List<String> permissions,
            boolean mfaVerified,
            long expiresAtEpochSecond
    ) {}
}
