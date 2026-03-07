package com.stablecoin.payments.fx.domain.model;

import com.stablecoin.payments.fx.domain.statemachine.StateMachine;
import com.stablecoin.payments.fx.domain.statemachine.StateTransition;
import lombok.AccessLevel;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.stablecoin.payments.fx.domain.model.FxRateLockStatus.ACTIVE;
import static com.stablecoin.payments.fx.domain.model.FxRateLockStatus.CONSUMED;
import static com.stablecoin.payments.fx.domain.model.FxRateLockStatus.EXPIRED;
import static com.stablecoin.payments.fx.domain.model.FxRateLockTrigger.CONSUME;
import static com.stablecoin.payments.fx.domain.model.FxRateLockTrigger.EXPIRE;

@Builder(toBuilder = true, access = AccessLevel.PACKAGE)
public record FxRateLock(
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
        BigDecimal feeAmount,
        String sourceCountry,
        String targetCountry,
        FxRateLockStatus status,
        Instant lockedAt,
        Instant expiresAt,
        Instant consumedAt
) {

    private static final StateMachine<FxRateLockStatus, FxRateLockTrigger> STATE_MACHINE =
            new StateMachine<>(List.of(
                    new StateTransition<>(ACTIVE, CONSUME, CONSUMED),
                    new StateTransition<>(ACTIVE, EXPIRE, EXPIRED)
            ));

    public static final int DEFAULT_LOCK_TTL_SECONDS = 30;

    public FxRateLock {
        if (lockedRate != null && lockedRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("lockedRate must be > 0");
        }
    }

    public static FxRateLock fromQuote(FxQuote quote, UUID paymentId, UUID correlationId,
                                        String sourceCountry, String targetCountry) {
        if (!quote.isActive()) {
            throw new IllegalStateException("Cannot lock rate from non-active quote %s (status=%s)"
                    .formatted(quote.quoteId(), quote.status()));
        }
        var now = Instant.now();
        return FxRateLock.builder()
                .lockId(UUID.randomUUID())
                .quoteId(quote.quoteId())
                .paymentId(paymentId)
                .correlationId(correlationId)
                .fromCurrency(quote.fromCurrency())
                .toCurrency(quote.toCurrency())
                .sourceAmount(quote.sourceAmount())
                .targetAmount(quote.targetAmount())
                .lockedRate(quote.rate())
                .feeBps(quote.feeBps())
                .feeAmount(quote.feeAmount())
                .sourceCountry(sourceCountry)
                .targetCountry(targetCountry)
                .status(ACTIVE)
                .lockedAt(now)
                .expiresAt(now.plusSeconds(DEFAULT_LOCK_TTL_SECONDS))
                .build();
    }

    public FxRateLock consume(UUID consumingPaymentId) {
        ensureNotExpired();
        if (!paymentId.equals(consumingPaymentId)) {
            throw new IllegalArgumentException(
                    "Lock %s belongs to payment %s, not %s".formatted(lockId, paymentId, consumingPaymentId));
        }
        var newStatus = STATE_MACHINE.transition(status, CONSUME);
        return toBuilder()
                .status(newStatus)
                .consumedAt(Instant.now())
                .build();
    }

    public FxRateLock expire() {
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
            throw new IllegalStateException("Lock %s has expired at %s".formatted(lockId, expiresAt));
        }
    }
}
