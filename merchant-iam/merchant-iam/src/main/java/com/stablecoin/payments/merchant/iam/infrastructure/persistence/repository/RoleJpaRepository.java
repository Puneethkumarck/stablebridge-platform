package com.stablecoin.payments.merchant.iam.infrastructure.persistence.repository;

import com.stablecoin.payments.merchant.iam.infrastructure.persistence.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleJpaRepository extends JpaRepository<RoleEntity, UUID> {

    List<RoleEntity> findByMerchantIdAndActiveTrue(UUID merchantId);

    Optional<RoleEntity> findByMerchantIdAndRoleName(UUID merchantId, String roleName);

    boolean existsByMerchantIdAndRoleName(UUID merchantId, String roleName);
}
