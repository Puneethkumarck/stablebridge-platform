package com.stablecoin.payments.compliance.infrastructure.persistence.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AmlResultJpaRepository extends JpaRepository<AmlResultEntity, UUID> {

    Optional<AmlResultEntity> findByCheckId(UUID checkId);
}
