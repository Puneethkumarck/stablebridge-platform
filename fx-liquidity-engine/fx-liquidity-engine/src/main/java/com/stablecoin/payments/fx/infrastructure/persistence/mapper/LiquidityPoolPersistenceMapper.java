package com.stablecoin.payments.fx.infrastructure.persistence.mapper;

import com.stablecoin.payments.fx.domain.model.LiquidityPool;
import com.stablecoin.payments.fx.infrastructure.persistence.entity.LiquidityPoolEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface LiquidityPoolPersistenceMapper {

    @Mapping(target = "version", ignore = true)
    LiquidityPoolEntity toEntity(LiquidityPool pool);

    default LiquidityPool toDomain(LiquidityPoolEntity entity) {
        if (entity == null) {
            return null;
        }
        return new LiquidityPool(
                entity.getPoolId(),
                entity.getFromCurrency(),
                entity.getToCurrency(),
                entity.getAvailableBalance(),
                entity.getReservedBalance(),
                entity.getMinimumThreshold(),
                entity.getMaximumCapacity(),
                entity.getUpdatedAt()
        );
    }
}
