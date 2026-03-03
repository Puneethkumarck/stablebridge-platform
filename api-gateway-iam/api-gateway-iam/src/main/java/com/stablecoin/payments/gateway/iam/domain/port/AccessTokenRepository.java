package com.stablecoin.payments.gateway.iam.domain.port;

import java.util.Optional;
import java.util.UUID;

import com.stablecoin.payments.gateway.iam.domain.model.AccessToken;

public interface AccessTokenRepository {

    AccessToken save(AccessToken token);

    Optional<AccessToken> findByJti(UUID jti);

    void revokeAllByMerchantId(UUID merchantId);

    void deleteExpiredBefore(java.time.Instant cutoff);
}
