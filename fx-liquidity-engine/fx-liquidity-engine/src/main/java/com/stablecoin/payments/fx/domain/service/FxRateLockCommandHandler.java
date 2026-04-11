package com.stablecoin.payments.fx.domain.service;

import com.stablecoin.payments.fx.domain.event.FxRateLocked;
import com.stablecoin.payments.fx.domain.exception.InsufficientLiquidityException;
import com.stablecoin.payments.fx.domain.exception.LockNotFoundException;
import com.stablecoin.payments.fx.domain.exception.PoolNotFoundException;
import com.stablecoin.payments.fx.domain.exception.QuoteAlreadyLockedException;
import com.stablecoin.payments.fx.domain.exception.QuoteExpiredException;
import com.stablecoin.payments.fx.domain.exception.QuoteNotFoundException;
import com.stablecoin.payments.fx.domain.model.FxQuote;
import com.stablecoin.payments.fx.domain.model.FxQuoteStatus;
import com.stablecoin.payments.fx.domain.model.FxRateLock;
import com.stablecoin.payments.fx.domain.model.FxRateLockStatus;
import com.stablecoin.payments.fx.domain.port.EventPublisher;
import com.stablecoin.payments.fx.domain.port.FxQuoteRepository;
import com.stablecoin.payments.fx.domain.port.FxRateLockRepository;
import com.stablecoin.payments.fx.domain.port.LiquidityPoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FxRateLockCommandHandler {

    private final FxQuoteRepository quoteRepository;
    private final FxRateLockRepository lockRepository;
    private final LiquidityPoolRepository poolRepository;
    private final LockService lockService;
    private final LiquidityService liquidityService;
    private final EventPublisher<Object> eventPublisher;

    public record LockRateResult(FxRateLock lock, boolean created) {}

    @Transactional
    public LockRateResult lockRate(UUID quoteId, UUID paymentId, UUID correlationId,
                                    String sourceCountry, String targetCountry) {
        log.info("Locking rate for quote={} payment={}", quoteId, paymentId);

        var existingLock = lockRepository.findByPaymentId(paymentId);
        if (existingLock.isPresent()) {
            log.info("Idempotent lock return for payment={} lockId={}",
                    paymentId, existingLock.get().lockId());
            return new LockRateResult(existingLock.get(), false);
        }

        var quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> QuoteNotFoundException.withId(quoteId));

        validateQuote(quote);

        var pool = poolRepository.findByCorridor(quote.fromCurrency(), quote.toCurrency())
                .orElseThrow(() -> PoolNotFoundException.forCorridor(
                        quote.fromCurrency(), quote.toCurrency()));

        if (!pool.hasSufficientLiquidity(quote.targetAmount())) {
            throw InsufficientLiquidityException.forCorridor(
                    quote.fromCurrency(), quote.toCurrency(),
                    quote.targetAmount(), pool.availableBalance());
        }

        var lockResult = lockService.lockRate(
                quote, paymentId, correlationId, sourceCountry, targetCountry, pool);

        quoteRepository.save(lockResult.lockedQuote());
        var savedLock = lockRepository.save(lockResult.lock());
        poolRepository.save(lockResult.updatedPool());

        publishFxRateLockedEvent(savedLock, correlationId);

        log.info("Rate locked: lockId={} rate={} expires={}",
                savedLock.lockId(), savedLock.lockedRate(), savedLock.expiresAt());

        return new LockRateResult(savedLock, true);
    }

    @Transactional
    public void releaseLock(UUID lockId) {
        log.info("Releasing lock lockId={}", lockId);

        var lock = lockRepository.findById(lockId)
                .orElseThrow(() -> LockNotFoundException.withId(lockId));

        if (lock.status() != FxRateLockStatus.ACTIVE) {
            log.info("Lock {} already in status {}, skipping release", lockId, lock.status());
            return;
        }

        var expiredLock = lock.expire();
        lockRepository.save(expiredLock);

        poolRepository.findByCorridor(lock.fromCurrency(), lock.toCurrency())
                .ifPresentOrElse(
                        pool -> {
                            var releasedPool = liquidityService.release(pool, lock.targetAmount());
                            poolRepository.save(releasedPool);
                            log.info("Lock {} released, liquidity returned to pool", lockId);
                        },
                        () -> log.warn("Lock {} released but pool not found for {}/{}",
                                lockId, lock.fromCurrency(), lock.toCurrency()));
    }

    private void validateQuote(FxQuote quote) {
        if (quote.isExpired()) {
            throw QuoteExpiredException.withId(quote.quoteId());
        }
        if (quote.status() == FxQuoteStatus.LOCKED) {
            throw QuoteAlreadyLockedException.withId(quote.quoteId());
        }
    }

    private void publishFxRateLockedEvent(FxRateLock lock, UUID correlationId) {
        var event = new FxRateLocked(
                lock.lockId(),
                lock.quoteId(),
                lock.paymentId(),
                correlationId,
                lock.fromCurrency(),
                lock.toCurrency(),
                lock.sourceAmount(),
                lock.targetAmount(),
                lock.lockedRate(),
                lock.feeBps(),
                lock.lockedAt(),
                lock.expiresAt()
        );
        eventPublisher.publish(event);
    }
}
