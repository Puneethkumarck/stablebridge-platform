package com.stablecoin.payments.fx.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FxRateLocked(
        UUID lockId,
        UUID quoteId,
        UUID paymentId,
        UUID correlationId,
        String fromCurrency,
        String toCurrency,
        BigDecimal sourceAmount,
        BigDecimal targetAmount,
        BigDecimal lockedRate,
        int feeBps,
        Instant lockedAt,
        Instant expiresAt
) {
    public static final String TOPIC = "fx.rate.locked";
}
