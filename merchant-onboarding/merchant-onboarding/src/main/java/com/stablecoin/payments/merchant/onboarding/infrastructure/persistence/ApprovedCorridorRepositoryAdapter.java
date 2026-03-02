package com.stablecoin.payments.merchant.onboarding.infrastructure.persistence;

import com.stablecoin.payments.merchant.onboarding.domain.merchant.ApprovedCorridorRepository;
import com.stablecoin.payments.merchant.onboarding.domain.merchant.model.core.ApprovedCorridor;
import com.stablecoin.payments.merchant.onboarding.infrastructure.persistence.entity.ApprovedCorridorEntity;
import com.stablecoin.payments.merchant.onboarding.infrastructure.persistence.entity.ApprovedCorridorJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ApprovedCorridorRepositoryAdapter implements ApprovedCorridorRepository {

    private final ApprovedCorridorJpaRepository jpa;

    @Override
    public ApprovedCorridor save(ApprovedCorridor corridor) {
        var entity = toEntity(corridor);
        var saved = jpa.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<ApprovedCorridor> findByMerchantId(UUID merchantId) {
        return jpa.findByMerchantId(merchantId).stream()
                .map(this::toDomain)
                .toList();
    }

    private ApprovedCorridorEntity toEntity(ApprovedCorridor corridor) {
        return ApprovedCorridorEntity.builder()
                .corridorId(corridor.corridorId())
                .merchantId(corridor.merchantId())
                .sourceCountry(corridor.sourceCountry())
                .targetCountry(corridor.targetCountry())
                .currencies(corridor.currencies())
                .maxAmountUsd(corridor.maxAmountUsd())
                .approvedBy(corridor.approvedBy())
                .approvedAt(corridor.approvedAt())
                .expiresAt(corridor.expiresAt())
                .isActive(corridor.isActive())
                .build();
    }

    private ApprovedCorridor toDomain(ApprovedCorridorEntity entity) {
        return ApprovedCorridor.builder()
                .corridorId(entity.getCorridorId())
                .merchantId(entity.getMerchantId())
                .sourceCountry(entity.getSourceCountry())
                .targetCountry(entity.getTargetCountry())
                .currencies(entity.getCurrencies())
                .maxAmountUsd(entity.getMaxAmountUsd())
                .approvedBy(entity.getApprovedBy())
                .approvedAt(entity.getApprovedAt())
                .expiresAt(entity.getExpiresAt())
                .isActive(entity.isActive())
                .build();
    }
}
