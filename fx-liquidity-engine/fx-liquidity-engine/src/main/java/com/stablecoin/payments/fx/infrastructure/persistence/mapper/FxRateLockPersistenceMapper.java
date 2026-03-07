package com.stablecoin.payments.fx.infrastructure.persistence.mapper;

import com.stablecoin.payments.fx.domain.model.FxRateLock;
import com.stablecoin.payments.fx.infrastructure.persistence.entity.FxRateLockEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface FxRateLockPersistenceMapper {

    @Mapping(target = "version", ignore = true)
    FxRateLockEntity toEntity(FxRateLock lock);

    default FxRateLock toDomain(FxRateLockEntity entity) {
        if (entity == null) {
            return null;
        }
        return new FxRateLock(
                entity.getLockId(),
                entity.getQuoteId(),
                entity.getPaymentId(),
                entity.getCorrelationId(),
                entity.getFromCurrency(),
                entity.getToCurrency(),
                entity.getSourceAmount(),
                entity.getTargetAmount(),
                entity.getLockedRate(),
                entity.getFeeBps(),
                entity.getFeeAmount(),
                entity.getSourceCountry(),
                entity.getTargetCountry(),
                entity.getStatus(),
                entity.getLockedAt(),
                entity.getExpiresAt(),
                entity.getConsumedAt()
        );
    }
}
