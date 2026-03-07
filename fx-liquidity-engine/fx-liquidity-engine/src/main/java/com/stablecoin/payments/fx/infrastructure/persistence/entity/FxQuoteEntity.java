package com.stablecoin.payments.fx.infrastructure.persistence.entity;

import com.stablecoin.payments.fx.domain.model.FxQuoteStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fx_quotes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FxQuoteEntity {

    @Id
    @Column(name = "quote_id", updatable = false, nullable = false)
    private UUID quoteId;

    @Column(name = "from_currency", nullable = false, length = 3)
    private String fromCurrency;

    @Column(name = "to_currency", nullable = false, length = 3)
    private String toCurrency;

    @Column(name = "source_amount", nullable = false, precision = 20, scale = 8)
    private BigDecimal sourceAmount;

    @Column(name = "target_amount", nullable = false, precision = 20, scale = 8)
    private BigDecimal targetAmount;

    @Column(name = "rate", nullable = false, precision = 20, scale = 10)
    private BigDecimal rate;

    @Column(name = "inverse_rate", nullable = false, precision = 20, scale = 10)
    private BigDecimal inverseRate;

    @Column(name = "fee_bps", nullable = false)
    private int feeBps;

    @Column(name = "fee_amount", nullable = false, precision = 20, scale = 8)
    private BigDecimal feeAmount;

    @Column(name = "provider", nullable = false, length = 100)
    private String provider;

    @Column(name = "provider_ref", length = 200)
    private String providerRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private FxQuoteStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Version
    @Column(name = "version")
    private Long version;
}
