package com.stablecoin.payments.orchestrator.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FxRate(
        UUID quoteId,
        String from,
        String to,
        BigDecimal rate,
        Instant lockedAt,
        Instant expiresAt,
        String provider
) {

    public FxRate {
        if (quoteId == null) {
            throw new IllegalArgumentException("quoteId is required");
        }
        if (from == null || from.isBlank()) {
            throw new IllegalArgumentException("from currency is required");
        }
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("to currency is required");
        }
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Rate must be positive");
        }
        if (lockedAt == null) {
            throw new IllegalArgumentException("lockedAt is required");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt is required");
        }
        if (!expiresAt.isAfter(lockedAt)) {
            throw new IllegalArgumentException("expiresAt must be after lockedAt");
        }
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("provider is required");
        }
    }

    public boolean isExpired(Instant at) {
        return at.isAfter(expiresAt);
    }
}
