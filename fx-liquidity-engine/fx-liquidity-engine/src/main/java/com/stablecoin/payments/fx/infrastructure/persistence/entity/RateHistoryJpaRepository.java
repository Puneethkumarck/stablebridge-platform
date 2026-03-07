package com.stablecoin.payments.fx.infrastructure.persistence.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RateHistoryJpaRepository extends JpaRepository<RateHistoryEntity, Long> {
    List<RateHistoryEntity> findByFromCurrencyAndToCurrencyOrderByRecordedAtDesc(
            String fromCurrency, String toCurrency);
}
