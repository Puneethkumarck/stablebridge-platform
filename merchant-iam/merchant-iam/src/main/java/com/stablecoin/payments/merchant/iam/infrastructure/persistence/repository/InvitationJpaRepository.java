package com.stablecoin.payments.merchant.iam.infrastructure.persistence.repository;

import com.stablecoin.payments.merchant.iam.infrastructure.persistence.entity.InvitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvitationJpaRepository extends JpaRepository<InvitationEntity, UUID> {

    Optional<InvitationEntity> findByTokenHash(String tokenHash);

    List<InvitationEntity> findByMerchantIdAndStatus(UUID merchantId, String status);

    @Modifying
    @Query("UPDATE InvitationEntity i SET i.status = 'EXPIRED' WHERE i.status = 'PENDING' AND i.expiresAt < :now")
    int expireOverdueInvitations(@Param("now") Instant now);
}
