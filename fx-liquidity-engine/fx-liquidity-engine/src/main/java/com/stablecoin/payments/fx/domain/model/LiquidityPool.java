package com.stablecoin.payments.fx.domain.model;

import lombok.AccessLevel;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder(toBuilder = true, access = AccessLevel.PACKAGE)
public record LiquidityPool(
        UUID poolId,
        String fromCurrency,
        String toCurrency,
        BigDecimal availableBalance,
        BigDecimal reservedBalance,
        BigDecimal minimumThreshold,
        BigDecimal maximumCapacity,
        Instant updatedAt
) {

    public LiquidityPool {
        if (availableBalance != null && availableBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Available balance must be non-negative");
        }
        if (reservedBalance != null && reservedBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Reserved balance must be non-negative");
        }
    }

    public static LiquidityPool create(String fromCurrency, String toCurrency,
                                        BigDecimal initialBalance, BigDecimal minimumThreshold,
                                        BigDecimal maximumCapacity) {
        return LiquidityPool.builder()
                .poolId(UUID.randomUUID())
                .fromCurrency(fromCurrency)
                .toCurrency(toCurrency)
                .availableBalance(initialBalance)
                .reservedBalance(BigDecimal.ZERO)
                .minimumThreshold(minimumThreshold)
                .maximumCapacity(maximumCapacity)
                .updatedAt(Instant.now())
                .build();
    }

    public LiquidityPool reserve(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Reserve amount must be positive");
        }
        if (availableBalance.compareTo(amount) < 0) {
            throw new IllegalStateException(
                    "Insufficient liquidity in pool %s: available=%s, requested=%s"
                            .formatted(poolId, availableBalance, amount));
        }
        return toBuilder()
                .availableBalance(availableBalance.subtract(amount))
                .reservedBalance(reservedBalance.add(amount))
                .updatedAt(Instant.now())
                .build();
    }

    public LiquidityPool release(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Release amount must be positive");
        }
        if (reservedBalance.compareTo(amount) < 0) {
            throw new IllegalStateException(
                    "Cannot release more than reserved in pool %s: reserved=%s, requested=%s"
                            .formatted(poolId, reservedBalance, amount));
        }
        return toBuilder()
                .availableBalance(availableBalance.add(amount))
                .reservedBalance(reservedBalance.subtract(amount))
                .updatedAt(Instant.now())
                .build();
    }

    public LiquidityPool consumeReservation(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Consume amount must be positive");
        }
        if (reservedBalance.compareTo(amount) < 0) {
            throw new IllegalStateException(
                    "Cannot consume more than reserved in pool %s: reserved=%s, requested=%s"
                            .formatted(poolId, reservedBalance, amount));
        }
        return toBuilder()
                .reservedBalance(reservedBalance.subtract(amount))
                .updatedAt(Instant.now())
                .build();
    }

    public BigDecimal totalBalance() {
        return availableBalance.add(reservedBalance);
    }

    public boolean isBelowThreshold() {
        return minimumThreshold != null && availableBalance.compareTo(minimumThreshold) < 0;
    }

    public boolean hasSufficientLiquidity(BigDecimal amount) {
        return availableBalance.compareTo(amount) >= 0;
    }

    public String corridor() {
        return fromCurrency + ":" + toCurrency;
    }
}
