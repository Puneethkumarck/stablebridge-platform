package com.stablecoin.payments.merchant.iam.infrastructure.persistence.repository;

import com.stablecoin.payments.merchant.iam.infrastructure.persistence.entity.UserSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface UserSessionJpaRepository extends JpaRepository<UserSessionEntity, UUID> {

    List<UserSessionEntity> findByUser_UserIdAndRevokedFalse(UUID userId);

    @Modifying
    @Query("UPDATE UserSessionEntity s SET s.revoked = true, s.revokedAt = CURRENT_TIMESTAMP, s.revokeReason = :reason WHERE s.user.userId = :userId AND s.revoked = false")
    int revokeAllByUserId(@Param("userId") UUID userId, @Param("reason") String reason);

    @Modifying
    @Query("UPDATE UserSessionEntity s SET s.revoked = true, s.revokedAt = CURRENT_TIMESTAMP, s.revokeReason = :reason WHERE s.merchantId = :merchantId AND s.revoked = false")
    int revokeAllByMerchantId(@Param("merchantId") UUID merchantId, @Param("reason") String reason);

    @Modifying
    @Query("DELETE FROM UserSessionEntity s WHERE s.expiresAt < :now AND s.revoked = false")
    int deleteExpiredSessions(@Param("now") Instant now);
}
