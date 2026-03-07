package com.stablecoin.payments.compliance.infrastructure.persistence;

import com.stablecoin.payments.compliance.domain.model.CustomerRiskProfile;
import com.stablecoin.payments.compliance.domain.port.CustomerRiskProfileRepository;
import com.stablecoin.payments.compliance.infrastructure.persistence.entity.CustomerRiskProfileJpaRepository;
import com.stablecoin.payments.compliance.infrastructure.persistence.mapper.CustomerRiskProfileEntityUpdater;
import com.stablecoin.payments.compliance.infrastructure.persistence.mapper.CustomerRiskProfilePersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomerRiskProfilePersistenceAdapter implements CustomerRiskProfileRepository {

    private final CustomerRiskProfileJpaRepository jpa;
    private final CustomerRiskProfilePersistenceMapper mapper;
    private final CustomerRiskProfileEntityUpdater updater;

    @Override
    public CustomerRiskProfile save(CustomerRiskProfile profile) {
        var existing = jpa.findById(profile.customerId());
        if (existing.isPresent()) {
            updater.updateEntity(existing.get(), profile);
            return mapper.toDomain(jpa.save(existing.get()));
        }
        return mapper.toDomain(jpa.save(mapper.toEntity(profile)));
    }

    @Override
    public Optional<CustomerRiskProfile> findByCustomerId(UUID customerId) {
        return jpa.findByCustomerId(customerId).map(mapper::toDomain);
    }
}
