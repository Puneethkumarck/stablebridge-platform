package com.stablecoin.payments.fx.infrastructure.persistence.entity;

import com.stablecoin.payments.fx.domain.model.FxRateLockStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FxRateLockJpaRepository extends JpaRepository<FxRateLockEntity, UUID> {
    Optional<FxRateLockEntity> findByPaymentId(UUID paymentId);
    List<FxRateLockEntity> findByStatusAndExpiresAtBefore(FxRateLockStatus status, Instant cutoff);
}
