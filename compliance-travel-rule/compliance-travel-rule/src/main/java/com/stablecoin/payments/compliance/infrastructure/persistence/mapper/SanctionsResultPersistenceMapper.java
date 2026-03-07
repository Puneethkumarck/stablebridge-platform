package com.stablecoin.payments.compliance.infrastructure.persistence.mapper;

import com.stablecoin.payments.compliance.domain.model.SanctionsResult;
import com.stablecoin.payments.compliance.infrastructure.persistence.entity.SanctionsResultEntity;
import org.mapstruct.Mapper;

@Mapper
public interface SanctionsResultPersistenceMapper {

    SanctionsResultEntity toEntity(SanctionsResult sanctionsResult);

    SanctionsResult toDomain(SanctionsResultEntity entity);
}
