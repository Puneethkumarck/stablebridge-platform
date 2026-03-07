package com.stablecoin.payments.fx.domain.event;

import java.time.Instant;
import java.util.UUID;

public record FxRateExpired(
        UUID lockId,
        UUID paymentId,
        Instant expiredAt
) {
    public static final String TOPIC = "fx.rate.expired";
}
