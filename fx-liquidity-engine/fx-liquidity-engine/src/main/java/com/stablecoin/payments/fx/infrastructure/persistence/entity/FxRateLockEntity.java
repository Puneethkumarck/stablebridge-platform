package com.stablecoin.payments.fx.infrastructure.persistence.entity;

import com.stablecoin.payments.fx.domain.model.FxRateLockStatus;
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
@Table(name = "fx_rate_locks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FxRateLockEntity {

    @Id
    @Column(name = "lock_id", updatable = false, nullable = false)
    private UUID lockId;

    @Column(name = "quote_id", nullable = false, updatable = false)
    private UUID quoteId;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "from_currency", nullable = false, length = 3)
    private String fromCurrency;

    @Column(name = "to_currency", nullable = false, length = 3)
    private String toCurrency;

    @Column(name = "source_amount", nullable = false, precision = 20, scale = 8)
    private BigDecimal sourceAmount;

    @Column(name = "target_amount", nullable = false, precision = 20, scale = 8)
    private BigDecimal targetAmount;

    @Column(name = "locked_rate", nullable = false, precision = 20, scale = 10)
    private BigDecimal lockedRate;

    @Column(name = "fee_bps", nullable = false)
    private int feeBps;

    @Column(name = "fee_amount", nullable = false, precision = 20, scale = 8)
    private BigDecimal feeAmount;

    @Column(name = "source_country", nullable = false, length = 2)
    private String sourceCountry;

    @Column(name = "target_country", nullable = false, length = 2)
    private String targetCountry;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private FxRateLockStatus status;

    @Column(name = "locked_at", nullable = false, updatable = false)
    private Instant lockedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Version
    @Column(name = "version")
    private Long version;
}
