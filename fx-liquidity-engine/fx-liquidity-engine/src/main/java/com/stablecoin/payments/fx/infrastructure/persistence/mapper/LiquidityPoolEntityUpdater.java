package com.stablecoin.payments.fx.infrastructure.persistence.mapper;

import com.stablecoin.payments.fx.domain.model.LiquidityPool;
import com.stablecoin.payments.fx.infrastructure.persistence.entity.LiquidityPoolEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface LiquidityPoolEntityUpdater {

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "poolId", ignore = true)
    void updateEntity(@MappingTarget LiquidityPoolEntity entity, LiquidityPool pool);
}
