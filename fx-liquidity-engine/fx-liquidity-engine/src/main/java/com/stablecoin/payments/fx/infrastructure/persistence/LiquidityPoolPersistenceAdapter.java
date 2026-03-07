package com.stablecoin.payments.fx.infrastructure.persistence;

import com.stablecoin.payments.fx.domain.model.LiquidityPool;
import com.stablecoin.payments.fx.domain.port.LiquidityPoolRepository;
import com.stablecoin.payments.fx.infrastructure.persistence.entity.LiquidityPoolJpaRepository;
import com.stablecoin.payments.fx.infrastructure.persistence.mapper.LiquidityPoolEntityUpdater;
import com.stablecoin.payments.fx.infrastructure.persistence.mapper.LiquidityPoolPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class LiquidityPoolPersistenceAdapter implements LiquidityPoolRepository {

    private final LiquidityPoolJpaRepository jpa;
    private final LiquidityPoolPersistenceMapper mapper;
    private final LiquidityPoolEntityUpdater updater;

    @Override
    public LiquidityPool save(LiquidityPool pool) {
        var existing = jpa.findById(pool.poolId());
        if (existing.isPresent()) {
            updater.updateEntity(existing.get(), pool);
            return mapper.toDomain(jpa.save(existing.get()));
        }
        return mapper.toDomain(jpa.save(mapper.toEntity(pool)));
    }

    @Override
    public Optional<LiquidityPool> findById(UUID poolId) {
        return jpa.findById(poolId).map(mapper::toDomain);
    }

    @Override
    public Optional<LiquidityPool> findByCorridor(String fromCurrency, String toCurrency) {
        return jpa.findByFromCurrencyAndToCurrency(fromCurrency, toCurrency).map(mapper::toDomain);
    }

    @Override
    public List<LiquidityPool> findAll() {
        return jpa.findAll().stream().map(mapper::toDomain).toList();
    }
}
