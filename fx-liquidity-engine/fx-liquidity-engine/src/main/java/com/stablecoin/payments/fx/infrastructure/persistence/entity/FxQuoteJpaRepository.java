package com.stablecoin.payments.fx.infrastructure.persistence.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FxQuoteJpaRepository extends JpaRepository<FxQuoteEntity, UUID> {
}
