package com.stablecoin.payments.merchant.iam.infrastructure.persistence.repository;

import com.stablecoin.payments.merchant.iam.infrastructure.persistence.entity.MerchantUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MerchantUserJpaRepository extends JpaRepository<MerchantUserEntity, UUID> {

    List<MerchantUserEntity> findByMerchantIdAndStatusNot(UUID merchantId, String status);

    Optional<MerchantUserEntity> findByMerchantIdAndEmailHash(UUID merchantId, String emailHash);

    Optional<MerchantUserEntity> findByEmailHash(String emailHash);

    boolean existsByMerchantIdAndEmailHash(UUID merchantId, String emailHash);

    long countByMerchantIdAndRole_RoleNameAndStatusNot(UUID merchantId, String roleName, String status);

    @Modifying
    @Query("UPDATE MerchantUserEntity u SET u.status = :status, u.deactivatedAt = CURRENT_TIMESTAMP, u.updatedAt = CURRENT_TIMESTAMP WHERE u.merchantId = :merchantId AND u.status <> 'DEACTIVATED'")
    int deactivateAllByMerchantId(@Param("merchantId") UUID merchantId, @Param("status") String status);
}
