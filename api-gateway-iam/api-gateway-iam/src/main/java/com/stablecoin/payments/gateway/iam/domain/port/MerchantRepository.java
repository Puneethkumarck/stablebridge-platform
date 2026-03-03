package com.stablecoin.payments.gateway.iam.domain.port;

import java.util.Optional;
import java.util.UUID;

import com.stablecoin.payments.gateway.iam.domain.model.Merchant;

public interface MerchantRepository {

    Merchant save(Merchant merchant);

    Optional<Merchant> findById(UUID merchantId);

    Optional<Merchant> findByExternalId(UUID externalId);

    boolean existsByExternalId(UUID externalId);
}
