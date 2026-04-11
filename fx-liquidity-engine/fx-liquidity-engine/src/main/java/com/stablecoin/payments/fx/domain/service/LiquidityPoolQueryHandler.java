package com.stablecoin.payments.fx.domain.service;

import com.stablecoin.payments.fx.domain.exception.PoolNotFoundException;
import com.stablecoin.payments.fx.domain.model.CorridorRate;
import com.stablecoin.payments.fx.domain.model.LiquidityPool;
import com.stablecoin.payments.fx.domain.port.LiquidityPoolRepository;
import com.stablecoin.payments.fx.domain.port.RateCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiquidityPoolQueryHandler {

    private final LiquidityPoolRepository poolRepository;
    private final RateCache rateCache;

    public record CorridorSnapshot(LiquidityPool pool, CorridorRate rate) {}

    @Transactional(readOnly = true)
    public List<LiquidityPool> listPools() {
        log.info("Listing all liquidity pools");
        return poolRepository.findAll();
    }

    @Transactional(readOnly = true)
    public LiquidityPool getPool(UUID poolId) {
        log.info("Getting liquidity pool: {}", poolId);
        return poolRepository.findById(poolId)
                .orElseThrow(() -> PoolNotFoundException.withId(poolId));
    }

    @Transactional(readOnly = true)
    public List<CorridorSnapshot> listCorridors() {
        log.info("Listing supported corridors with current rates");
        return poolRepository.findAll().stream()
                .map(pool -> new CorridorSnapshot(
                        pool,
                        rateCache.get(pool.fromCurrency(), pool.toCurrency()).orElse(null)))
                .toList();
    }
}
