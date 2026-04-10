package com.stablecoin.payments.custody.domain.port;

import com.stablecoin.payments.custody.domain.model.ChainId;

import java.util.Optional;
import java.util.UUID;

public interface NonceRepository {

    long assignNextNonce(UUID walletId, ChainId chainId);

    Optional<Long> getCurrentNonce(UUID walletId, ChainId chainId);
}
