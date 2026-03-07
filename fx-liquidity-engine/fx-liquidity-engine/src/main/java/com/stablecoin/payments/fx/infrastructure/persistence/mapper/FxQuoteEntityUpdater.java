package com.stablecoin.payments.fx.infrastructure.persistence.mapper;

import com.stablecoin.payments.fx.domain.model.FxQuote;
import com.stablecoin.payments.fx.infrastructure.persistence.entity.FxQuoteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface FxQuoteEntityUpdater {

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "quoteId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(@MappingTarget FxQuoteEntity entity, FxQuote quote);
}
