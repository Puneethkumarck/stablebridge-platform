package com.stablecoin.payments.compliance.infrastructure.persistence.mapper;

import com.stablecoin.payments.compliance.domain.model.KycResult;
import com.stablecoin.payments.compliance.infrastructure.persistence.entity.KycResultEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface KycResultPersistenceMapper {

    @Mapping(target = "rawResponse", ignore = true)
    KycResultEntity toEntity(KycResult kycResult);

    KycResult toDomain(KycResultEntity entity);
}
