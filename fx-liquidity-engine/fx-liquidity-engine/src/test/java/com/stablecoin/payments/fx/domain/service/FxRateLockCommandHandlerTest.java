package com.stablecoin.payments.fx.domain.service;

import com.stablecoin.payments.fx.domain.event.FxRateLocked;
import com.stablecoin.payments.fx.domain.exception.InsufficientLiquidityException;
import com.stablecoin.payments.fx.domain.exception.LockNotFoundException;
import com.stablecoin.payments.fx.domain.exception.PoolNotFoundException;
import com.stablecoin.payments.fx.domain.exception.QuoteAlreadyLockedException;
import com.stablecoin.payments.fx.domain.exception.QuoteExpiredException;
import com.stablecoin.payments.fx.domain.exception.QuoteNotFoundException;
import com.stablecoin.payments.fx.domain.port.EventPublisher;
import com.stablecoin.payments.fx.domain.port.FxQuoteRepository;
import com.stablecoin.payments.fx.domain.port.FxRateLockRepository;
import com.stablecoin.payments.fx.domain.port.LiquidityPoolRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static com.stablecoin.payments.fx.fixtures.FxQuoteFixtures.aLockedQuote;
import static com.stablecoin.payments.fx.fixtures.FxQuoteFixtures.anActiveQuote;
import static com.stablecoin.payments.fx.fixtures.FxQuoteFixtures.anExpiredQuote;
import static com.stablecoin.payments.fx.fixtures.FxRateLockFixtures.CORRELATION_ID;
import static com.stablecoin.payments.fx.fixtures.FxRateLockFixtures.PAYMENT_ID;
import static com.stablecoin.payments.fx.fixtures.FxRateLockFixtures.SOURCE_COUNTRY;
import static com.stablecoin.payments.fx.fixtures.FxRateLockFixtures.TARGET_COUNTRY;
import static com.stablecoin.payments.fx.fixtures.FxRateLockFixtures.anActiveLock;
import static com.stablecoin.payments.fx.fixtures.LiquidityPoolFixtures.aPoolWithLowBalance;
import static com.stablecoin.payments.fx.fixtures.LiquidityPoolFixtures.aUsdEurPool;
import static com.stablecoin.payments.platform.test.TestUtils.eqIgnoring;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("FxRateLockCommandHandler")
class FxRateLockCommandHandlerTest {

    @Mock
    private FxQuoteRepository quoteRepository;

    @Mock
    private FxRateLockRepository lockRepository;

    @Mock
    private LiquidityPoolRepository poolRepository;

    @Mock
    private LockService lockService;

    @Mock
    private LiquidityService liquidityService;

    @Mock
    private EventPublisher<Object> eventPublisher;

    @InjectMocks
    private FxRateLockCommandHandler handler;

    @Nested
    @DisplayName("lockRate")
    class LockRate {

        @Test
        void shouldLockRateSuccessfully() {
            var quote = anActiveQuote();
            var quoteId = quote.quoteId();
            var pool = aUsdEurPool();

            var lockedQuote = aLockedQuote();
            var lock = anActiveLock(quoteId, PAYMENT_ID);
            var updatedPool = aUsdEurPool();
            var lockResult = new LockService.LockResult(lockedQuote, lock, updatedPool);

            given(lockRepository.findByPaymentId(PAYMENT_ID)).willReturn(Optional.empty());
            given(quoteRepository.findById(quoteId)).willReturn(Optional.of(quote));
            given(poolRepository.findByCorridor("USD", "EUR")).willReturn(Optional.of(pool));
            given(lockService.lockRate(quote, PAYMENT_ID, CORRELATION_ID,
                    SOURCE_COUNTRY, TARGET_COUNTRY, pool)).willReturn(lockResult);
            given(quoteRepository.save(lockedQuote)).willReturn(lockedQuote);
            given(lockRepository.save(lock)).willReturn(lock);
            given(poolRepository.save(updatedPool)).willReturn(updatedPool);

            var result = handler.lockRate(quoteId, PAYMENT_ID, CORRELATION_ID,
                    SOURCE_COUNTRY, TARGET_COUNTRY);

            var expected = new FxRateLockCommandHandler.LockRateResult(lock, true);
            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);

            var expectedEvent = new FxRateLocked(
                    lock.lockId(), lock.quoteId(), lock.paymentId(), CORRELATION_ID,
                    lock.fromCurrency(), lock.toCurrency(),
                    lock.sourceAmount(), lock.targetAmount(), lock.lockedRate(),
                    lock.feeBps(), lock.lockedAt(), lock.expiresAt());
            then(eventPublisher).should().publish(eqIgnoring(expectedEvent));
        }

        @Test
        void shouldReturnExistingLockForSamePaymentId() {
            var quoteId = UUID.randomUUID();
            var existingLock = anActiveLock(quoteId, PAYMENT_ID);

            given(lockRepository.findByPaymentId(PAYMENT_ID)).willReturn(Optional.of(existingLock));

            var result = handler.lockRate(quoteId, PAYMENT_ID, CORRELATION_ID,
                    SOURCE_COUNTRY, TARGET_COUNTRY);

            var expected = new FxRateLockCommandHandler.LockRateResult(existingLock, false);
            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);

            then(quoteRepository).shouldHaveNoInteractions();
            then(lockService).shouldHaveNoInteractions();
            then(lockRepository).should().findByPaymentId(PAYMENT_ID);
            then(lockRepository).shouldHaveNoMoreInteractions();
            then(eventPublisher).shouldHaveNoInteractions();
        }

        @Test
        void shouldThrowWhenQuoteNotFound() {
            var quoteId = UUID.randomUUID();
            given(lockRepository.findByPaymentId(PAYMENT_ID)).willReturn(Optional.empty());
            given(quoteRepository.findById(quoteId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> handler.lockRate(quoteId, PAYMENT_ID, CORRELATION_ID,
                    SOURCE_COUNTRY, TARGET_COUNTRY))
                    .isInstanceOf(QuoteNotFoundException.class)
                    .hasMessageContaining(quoteId.toString());

            then(lockRepository).should().findByPaymentId(PAYMENT_ID);
            then(lockRepository).shouldHaveNoMoreInteractions();
            then(eventPublisher).shouldHaveNoInteractions();
        }

        @Test
        void shouldThrowWhenQuoteExpired() {
            var expiredQuote = anExpiredQuote();
            var quoteId = expiredQuote.quoteId();
            given(lockRepository.findByPaymentId(PAYMENT_ID)).willReturn(Optional.empty());
            given(quoteRepository.findById(quoteId)).willReturn(Optional.of(expiredQuote));

            assertThatThrownBy(() -> handler.lockRate(quoteId, PAYMENT_ID, CORRELATION_ID,
                    SOURCE_COUNTRY, TARGET_COUNTRY))
                    .isInstanceOf(QuoteExpiredException.class)
                    .hasMessageContaining(quoteId.toString());

            then(lockRepository).should().findByPaymentId(PAYMENT_ID);
            then(lockRepository).shouldHaveNoMoreInteractions();
            then(eventPublisher).shouldHaveNoInteractions();
        }

        @Test
        void shouldThrowWhenQuoteAlreadyLocked() {
            var lockedQuote = aLockedQuote();
            var quoteId = lockedQuote.quoteId();
            given(lockRepository.findByPaymentId(PAYMENT_ID)).willReturn(Optional.empty());
            given(quoteRepository.findById(quoteId)).willReturn(Optional.of(lockedQuote));

            assertThatThrownBy(() -> handler.lockRate(quoteId, PAYMENT_ID, CORRELATION_ID,
                    SOURCE_COUNTRY, TARGET_COUNTRY))
                    .isInstanceOf(QuoteAlreadyLockedException.class)
                    .hasMessageContaining(quoteId.toString());

            then(lockRepository).should().findByPaymentId(PAYMENT_ID);
            then(lockRepository).shouldHaveNoMoreInteractions();
            then(eventPublisher).shouldHaveNoInteractions();
        }

        @Test
        void shouldThrowWhenPoolNotFound() {
            var quote = anActiveQuote();
            var quoteId = quote.quoteId();
            given(lockRepository.findByPaymentId(PAYMENT_ID)).willReturn(Optional.empty());
            given(quoteRepository.findById(quoteId)).willReturn(Optional.of(quote));
            given(poolRepository.findByCorridor("USD", "EUR")).willReturn(Optional.empty());

            assertThatThrownBy(() -> handler.lockRate(quoteId, PAYMENT_ID, CORRELATION_ID,
                    SOURCE_COUNTRY, TARGET_COUNTRY))
                    .isInstanceOf(PoolNotFoundException.class)
                    .hasMessageContaining("USD:EUR");

            then(lockRepository).should().findByPaymentId(PAYMENT_ID);
            then(lockRepository).shouldHaveNoMoreInteractions();
            then(eventPublisher).shouldHaveNoInteractions();
        }

        @Test
        void shouldThrowWhenInsufficientLiquidity() {
            var quote = anActiveQuote();
            var quoteId = quote.quoteId();
            var lowPool = aPoolWithLowBalance();
            given(lockRepository.findByPaymentId(PAYMENT_ID)).willReturn(Optional.empty());
            given(quoteRepository.findById(quoteId)).willReturn(Optional.of(quote));
            given(poolRepository.findByCorridor("USD", "EUR")).willReturn(Optional.of(lowPool));

            assertThatThrownBy(() -> handler.lockRate(quoteId, PAYMENT_ID, CORRELATION_ID,
                    SOURCE_COUNTRY, TARGET_COUNTRY))
                    .isInstanceOf(InsufficientLiquidityException.class)
                    .hasMessageContaining("USD:EUR");

            then(lockRepository).should().findByPaymentId(PAYMENT_ID);
            then(lockRepository).shouldHaveNoMoreInteractions();
            then(eventPublisher).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("releaseLock")
    class ReleaseLock {

        @Test
        void shouldThrowWhenLockNotFound() {
            var lockId = UUID.randomUUID();
            given(lockRepository.findById(lockId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> handler.releaseLock(lockId))
                    .isInstanceOf(LockNotFoundException.class)
                    .hasMessageContaining(lockId.toString());

            then(lockRepository).should().findById(lockId);
            then(lockRepository).shouldHaveNoMoreInteractions();
            then(poolRepository).shouldHaveNoInteractions();
            then(liquidityService).shouldHaveNoInteractions();
            then(eventPublisher).shouldHaveNoInteractions();
        }

        @Test
        void shouldSkipReleaseWhenLockIsNotActive() {
            var activeLock = anActiveLock(UUID.randomUUID());
            var expiredLock = activeLock.expire();
            var lockId = expiredLock.lockId();
            given(lockRepository.findById(lockId)).willReturn(Optional.of(expiredLock));

            handler.releaseLock(lockId);

            then(lockRepository).should().findById(lockId);
            then(lockRepository).shouldHaveNoMoreInteractions();
            then(poolRepository).shouldHaveNoInteractions();
            then(liquidityService).shouldHaveNoInteractions();
            then(eventPublisher).shouldHaveNoInteractions();
        }

        @Test
        void shouldReleaseActiveLockAndReturnLiquidityToPool() {
            var activeLock = anActiveLock(UUID.randomUUID());
            var lockId = activeLock.lockId();
            var pool = aUsdEurPool();
            var releasedPool = aUsdEurPool();

            given(lockRepository.findById(lockId)).willReturn(Optional.of(activeLock));
            given(poolRepository.findByCorridor("USD", "EUR")).willReturn(Optional.of(pool));
            given(liquidityService.release(pool, activeLock.targetAmount())).willReturn(releasedPool);

            handler.releaseLock(lockId);

            var expectedExpiredLock = activeLock.expire();
            then(lockRepository).should().save(eqIgnoring(expectedExpiredLock));
            then(poolRepository).should().save(releasedPool);
            then(liquidityService).should().release(pool, activeLock.targetAmount());
            then(eventPublisher).shouldHaveNoInteractions();
        }

        @Test
        void shouldReleaseActiveLockButLogWarnWhenPoolMissing() {
            var activeLock = anActiveLock(UUID.randomUUID());
            var lockId = activeLock.lockId();

            given(lockRepository.findById(lockId)).willReturn(Optional.of(activeLock));
            given(poolRepository.findByCorridor("USD", "EUR")).willReturn(Optional.empty());

            handler.releaseLock(lockId);

            var expectedExpiredLock = activeLock.expire();
            then(lockRepository).should().save(eqIgnoring(expectedExpiredLock));
            then(poolRepository).should().findByCorridor("USD", "EUR");
            then(poolRepository).shouldHaveNoMoreInteractions();
            then(liquidityService).shouldHaveNoInteractions();
            then(eventPublisher).shouldHaveNoInteractions();
        }
    }
}
