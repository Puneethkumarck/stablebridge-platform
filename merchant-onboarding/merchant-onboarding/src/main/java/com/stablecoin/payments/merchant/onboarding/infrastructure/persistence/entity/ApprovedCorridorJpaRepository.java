package com.stablecoin.payments.merchant.onboarding.infrastructure.persistence.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApprovedCorridorJpaRepository extends JpaRepository<ApprovedCorridorEntity, UUID> {

    List<ApprovedCorridorEntity> findByMerchantId(UUID merchantId);

    boolean existsByMerchantIdAndSourceCountryAndTargetCountry(
            UUID merchantId, String sourceCountry, String targetCountry);
}
