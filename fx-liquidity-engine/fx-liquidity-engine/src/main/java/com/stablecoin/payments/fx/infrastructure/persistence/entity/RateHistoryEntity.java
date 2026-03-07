package com.stablecoin.payments.fx.infrastructure.persistence.entity;

import com.stablecoin.payments.fx.domain.model.RateSourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "rate_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @Column(name = "from_currency", nullable = false, length = 3)
    private String fromCurrency;

    @Column(name = "to_currency", nullable = false, length = 3)
    private String toCurrency;

    @Column(name = "rate", nullable = false, precision = 20, scale = 10)
    private BigDecimal rate;

    @Column(name = "bid", precision = 20, scale = 10)
    private BigDecimal bid;

    @Column(name = "ask", precision = 20, scale = 10)
    private BigDecimal ask;

    @Column(name = "provider", nullable = false, length = 100)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private RateSourceType sourceType;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;
}
