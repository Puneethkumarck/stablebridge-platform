package com.stablecoin.payments.fx.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LiquidityThresholdBreached(
        UUID poolId,
        String fromCurrency,
        String toCurrency,
        BigDecimal availableBalance,
        BigDecimal threshold,
        Instant breachedAt
) {
    public static final String TOPIC = "liquidity.threshold.breached";
}
