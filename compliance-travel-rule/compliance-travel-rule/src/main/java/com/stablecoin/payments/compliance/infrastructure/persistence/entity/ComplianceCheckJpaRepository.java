package com.stablecoin.payments.compliance.infrastructure.persistence.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ComplianceCheckJpaRepository extends JpaRepository<ComplianceCheckEntity, UUID> {

    Optional<ComplianceCheckEntity> findByPaymentId(UUID paymentId);
}
