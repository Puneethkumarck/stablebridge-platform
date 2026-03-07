package com.stablecoin.payments.fx.domain.model;

import lombok.Builder;

import java.math.BigDecimal;

@Builder(toBuilder = true)
public record CorridorRate(
        String fromCurrency,
        String toCurrency,
        BigDecimal rate,
        int spreadBps,
        int feeBps,
        String provider,
        int ageMs
) {
    public CorridorRate {
        if (ageMs > 5000) {
            throw new IllegalArgumentException("Rate must be less than 5 seconds old, but was " + ageMs + "ms");
        }
    }
}
