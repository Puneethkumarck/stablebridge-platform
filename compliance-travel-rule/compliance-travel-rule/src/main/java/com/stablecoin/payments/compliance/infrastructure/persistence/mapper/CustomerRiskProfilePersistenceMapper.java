package com.stablecoin.payments.compliance.infrastructure.persistence.mapper;

import com.stablecoin.payments.compliance.domain.model.CustomerRiskProfile;
import com.stablecoin.payments.compliance.infrastructure.persistence.entity.CustomerRiskProfileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface CustomerRiskProfilePersistenceMapper {

    @Mapping(target = "version", ignore = true)
    CustomerRiskProfileEntity toEntity(CustomerRiskProfile profile);

    CustomerRiskProfile toDomain(CustomerRiskProfileEntity entity);
}
