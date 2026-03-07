package com.stablecoin.payments.fx.infrastructure.persistence.mapper;

import com.stablecoin.payments.fx.domain.model.FxRateLock;
import com.stablecoin.payments.fx.infrastructure.persistence.entity.FxRateLockEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface FxRateLockEntityUpdater {

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "lockId", ignore = true)
    @Mapping(target = "quoteId", ignore = true)
    @Mapping(target = "lockedAt", ignore = true)
    void updateEntity(@MappingTarget FxRateLockEntity entity, FxRateLock lock);
}
