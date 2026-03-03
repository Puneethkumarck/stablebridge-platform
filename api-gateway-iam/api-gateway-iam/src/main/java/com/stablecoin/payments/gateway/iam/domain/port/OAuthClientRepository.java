package com.stablecoin.payments.gateway.iam.domain.port;

import java.util.Optional;
import java.util.UUID;

import com.stablecoin.payments.gateway.iam.domain.model.OAuthClient;

public interface OAuthClientRepository {

    OAuthClient save(OAuthClient client);

    Optional<OAuthClient> findById(UUID clientId);

    Optional<OAuthClient> findActiveById(UUID clientId);

    void deactivateAllByMerchantId(UUID merchantId);
}
