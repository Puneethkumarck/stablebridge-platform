package com.stablecoin.payments.merchant.iam.infrastructure.persistence.repository;

import com.stablecoin.payments.merchant.iam.infrastructure.persistence.entity.PermissionAuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PermissionAuditLogJpaRepository extends JpaRepository<PermissionAuditLogEntity, UUID> {

    Page<PermissionAuditLogEntity> findByMerchantIdOrderByOccurredAtDesc(UUID merchantId, Pageable pageable);

    Page<PermissionAuditLogEntity> findByUserIdOrderByOccurredAtDesc(UUID userId, Pageable pageable);
}
