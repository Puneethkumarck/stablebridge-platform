package com.stablecoin.payments.compliance.infrastructure.persistence.mapper;

import com.stablecoin.payments.compliance.domain.model.CustomerRiskProfile;
import com.stablecoin.payments.compliance.infrastructure.persistence.entity.CustomerRiskProfileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(uses = CustomerRiskProfilePersistenceMapper.class)
public interface CustomerRiskProfileEntityUpdater {

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(@MappingTarget CustomerRiskProfileEntity entity, CustomerRiskProfile profile);
}
