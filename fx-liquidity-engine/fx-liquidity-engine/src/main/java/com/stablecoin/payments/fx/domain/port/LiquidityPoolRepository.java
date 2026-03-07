package com.stablecoin.payments.fx.domain.port;

import com.stablecoin.payments.fx.domain.model.LiquidityPool;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LiquidityPoolRepository {
    LiquidityPool save(LiquidityPool pool);
    Optional<LiquidityPool> findById(UUID poolId);
    Optional<LiquidityPool> findByCorridor(String fromCurrency, String toCurrency);
    List<LiquidityPool> findAll();
}
