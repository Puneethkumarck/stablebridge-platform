package com.stablecoin.payments.fx.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "liquidity_pools")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiquidityPoolEntity {

    @Id
    @Column(name = "pool_id", updatable = false, nullable = false)
    private UUID poolId;

    @Column(name = "from_currency", nullable = false, length = 3)
    private String fromCurrency;

    @Column(name = "to_currency", nullable = false, length = 3)
    private String toCurrency;

    @Column(name = "available_balance", nullable = false, precision = 20, scale = 8)
    private BigDecimal availableBalance;

    @Column(name = "reserved_balance", nullable = false, precision = 20, scale = 8)
    private BigDecimal reservedBalance;

    @Column(name = "minimum_threshold", nullable = false, precision = 20, scale = 8)
    private BigDecimal minimumThreshold;

    @Column(name = "maximum_capacity", nullable = false, precision = 20, scale = 8)
    private BigDecimal maximumCapacity;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;
}
