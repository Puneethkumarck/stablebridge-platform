package com.stablecoin.payments.compliance.infrastructure.persistence.mapper;

import com.stablecoin.payments.compliance.domain.model.AmlResult;
import com.stablecoin.payments.compliance.infrastructure.persistence.entity.AmlResultEntity;
import org.mapstruct.Mapper;

@Mapper
public interface AmlResultPersistenceMapper {

    AmlResultEntity toEntity(AmlResult amlResult);

    AmlResult toDomain(AmlResultEntity entity);
}
