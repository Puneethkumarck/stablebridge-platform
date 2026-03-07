package com.stablecoin.payments.fx.domain.model;

import com.stablecoin.payments.fx.domain.statemachine.StateMachine;
import com.stablecoin.payments.fx.domain.statemachine.StateTransition;
import lombok.AccessLevel;
import lombok.Builder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.stablecoin.payments.fx.domain.model.FxQuoteStatus.ACTIVE;
import static com.stablecoin.payments.fx.domain.model.FxQuoteStatus.EXPIRED;
import static com.stablecoin.payments.fx.domain.model.FxQuoteStatus.LOCKED;
import static com.stablecoin.payments.fx.domain.model.FxQuoteTrigger.EXPIRE;
import static com.stablecoin.payments.fx.domain.model.FxQuoteTrigger.LOCK;

@Builder(toBuilder = true, access = AccessLevel.PACKAGE)
public record FxQuote(
        UUID quoteId,
        String fromCurrency,
        String toCurrency,
        BigDecimal sourceAmount,
        BigDecimal targetAmount,
        BigDecimal rate,
        BigDecimal inverseRate,
        int spreadBps,
        int feeBps,
        BigDecimal feeAmount,
        String provider,
        String providerRef,
        FxQuoteStatus status,
        Instant createdAt,
        Instant expiresAt
) {

    private static final StateMachine<FxQuoteStatus, FxQuoteTrigger> STATE_MACHINE =
            new StateMachine<>(List.of(
                    new StateTransition<>(ACTIVE, LOCK, LOCKED),
                    new StateTransition<>(ACTIVE, EXPIRE, EXPIRED)
            ));

    public static FxQuote create(String fromCurrency, String toCurrency, BigDecimal sourceAmount,
                                  CorridorRate corridorRate, int quoteTtlSeconds) {
        var rate = corridorRate.rate();
        var feeBps = corridorRate.feeBps();
        var spreadBps = corridorRate.spreadBps();

        // Apply spread to rate
        var spreadFactor = BigDecimal.ONE.subtract(BigDecimal.valueOf(spreadBps).movePointLeft(4));
        var effectiveRate = rate.multiply(spreadFactor);

        var targetAmount = sourceAmount.multiply(effectiveRate);
        var feeAmount = sourceAmount.multiply(BigDecimal.valueOf(feeBps).movePointLeft(4));
        var inverseRate = BigDecimal.ONE.divide(effectiveRate, 10, RoundingMode.HALF_UP);

        var now = Instant.now();
        return FxQuote.builder()
                .quoteId(UUID.randomUUID())
                .fromCurrency(fromCurrency)
                .toCurrency(toCurrency)
                .sourceAmount(sourceAmount)
                .targetAmount(targetAmount)
                .rate(effectiveRate)
                .inverseRate(inverseRate)
                .spreadBps(spreadBps)
                .feeBps(feeBps)
                .feeAmount(feeAmount)
                .provider(corridorRate.provider())
                .status(ACTIVE)
                .createdAt(now)
                .expiresAt(now.plusSeconds(quoteTtlSeconds))
                .build();
    }

    public FxQuote lock() {
        ensureNotExpired();
        var newStatus = STATE_MACHINE.transition(status, LOCK);
        return toBuilder().status(newStatus).build();
    }

    public FxQuote expire() {
        var newStatus = STATE_MACHINE.transition(status, EXPIRE);
        return toBuilder().status(newStatus).build();
    }

    public boolean isExpired() {
        return status == EXPIRED || (expiresAt != null && Instant.now().isAfter(expiresAt));
    }

    public boolean isActive() {
        return status == ACTIVE && !isExpired();
    }

    private void ensureNotExpired() {
        if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
            throw new IllegalStateException("Quote %s has expired at %s".formatted(quoteId, expiresAt));
        }
    }
}
