package com.stablecoin.payments.fx.infrastructure.persistence.mapper;

import com.stablecoin.payments.fx.domain.model.FxQuote;
import com.stablecoin.payments.fx.infrastructure.persistence.entity.FxQuoteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface FxQuotePersistenceMapper {

    @Mapping(target = "version", ignore = true)
    FxQuoteEntity toEntity(FxQuote quote);

    default FxQuote toDomain(FxQuoteEntity entity) {
        if (entity == null) {
            return null;
        }
        return new FxQuote(
                entity.getQuoteId(),
                entity.getFromCurrency(),
                entity.getToCurrency(),
                entity.getSourceAmount(),
                entity.getTargetAmount(),
                entity.getRate(),
                entity.getInverseRate(),
                0, // spreadBps — already baked into rate, not persisted
                entity.getFeeBps(),
                entity.getFeeAmount(),
                entity.getProvider(),
                entity.getProviderRef(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getExpiresAt()
        );
    }
}
