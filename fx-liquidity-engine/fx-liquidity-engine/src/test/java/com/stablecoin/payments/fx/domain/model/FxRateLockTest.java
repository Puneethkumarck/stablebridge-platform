package com.stablecoin.payments.fx.domain.model;

import com.stablecoin.payments.fx.domain.statemachine.StateMachineException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static com.stablecoin.payments.fx.domain.model.FxRateLockStatus.ACTIVE;
import static com.stablecoin.payments.fx.domain.model.FxRateLockStatus.CONSUMED;
import static com.stablecoin.payments.fx.domain.model.FxRateLockStatus.EXPIRED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FxRateLock")
class FxRateLockTest {

    private static final String FROM_CURRENCY = "USD";
    private static final String TO_CURRENCY = "EUR";
    private static final String SOURCE_COUNTRY = "US";
    private static final String TARGET_COUNTRY = "DE";
    private static final BigDecimal SOURCE_AMOUNT = new BigDecimal("1000");
    private static final BigDecimal RATE = new BigDecimal("0.92");
    private static final int SPREAD_BPS = 30;
    private static final int FEE_BPS = 30;
    private static final String PROVIDER = "test-provider";

    private FxQuote activeQuote() {
        var corridorRate = CorridorRate.builder()
                .fromCurrency(FROM_CURRENCY)
                .toCurrency(TO_CURRENCY)
                .rate(RATE)
                .spreadBps(SPREAD_BPS)
                .feeBps(FEE_BPS)
                .provider(PROVIDER)
                .ageMs(100)
                .build();
        return FxQuote.create(FROM_CURRENCY, TO_CURRENCY, SOURCE_AMOUNT, corridorRate, 10);
    }

    private FxRateLock activeLock() {
        var quote = activeQuote();
        var paymentId = UUID.randomUUID();
        var correlationId = UUID.randomUUID();
        return FxRateLock.fromQuote(quote, paymentId, correlationId, SOURCE_COUNTRY, TARGET_COUNTRY);
    }

    private FxRateLock expiredTimeLock() {
        return FxRateLock.builder()
                .lockId(UUID.randomUUID())
                .quoteId(UUID.randomUUID())
                .paymentId(UUID.randomUUID())
                .correlationId(UUID.randomUUID())
                .fromCurrency(FROM_CURRENCY)
                .toCurrency(TO_CURRENCY)
                .sourceAmount(SOURCE_AMOUNT)
                .targetAmount(new BigDecimal("920"))
                .lockedRate(RATE)
                .feeBps(FEE_BPS)
                .feeAmount(new BigDecimal("3"))
                .sourceCountry(SOURCE_COUNTRY)
                .targetCountry(TARGET_COUNTRY)
                .status(ACTIVE)
                .lockedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().minusSeconds(10))
                .build();
    }

    @Nested
    @DisplayName("compact constructor")
    class CompactConstructor {

        @Test
        @DisplayName("should throw IllegalArgumentException when lockedRate is zero")
        void should_throwIllegalArgumentException_when_lockedRateIsZero() {
            assertThatThrownBy(() -> FxRateLock.builder()
                    .lockId(UUID.randomUUID())
                    .quoteId(UUID.randomUUID())
                    .paymentId(UUID.randomUUID())
                    .correlationId(UUID.randomUUID())
                    .fromCurrency(FROM_CURRENCY)
                    .toCurrency(TO_CURRENCY)
                    .sourceAmount(SOURCE_AMOUNT)
                    .targetAmount(new BigDecimal("920"))
                    .lockedRate(BigDecimal.ZERO)
                    .feeBps(FEE_BPS)
                    .feeAmount(new BigDecimal("3"))
                    .sourceCountry(SOURCE_COUNTRY)
                    .targetCountry(TARGET_COUNTRY)
                    .status(ACTIVE)
                    .lockedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(30))
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("lockedRate must be > 0");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when lockedRate is negative")
        void should_throwIllegalArgumentException_when_lockedRateIsNegative() {
            assertThatThrownBy(() -> FxRateLock.builder()
                    .lockId(UUID.randomUUID())
                    .quoteId(UUID.randomUUID())
                    .paymentId(UUID.randomUUID())
                    .correlationId(UUID.randomUUID())
                    .fromCurrency(FROM_CURRENCY)
                    .toCurrency(TO_CURRENCY)
                    .sourceAmount(SOURCE_AMOUNT)
                    .targetAmount(new BigDecimal("920"))
                    .lockedRate(new BigDecimal("-0.5"))
                    .feeBps(FEE_BPS)
                    .feeAmount(new BigDecimal("3"))
                    .sourceCountry(SOURCE_COUNTRY)
                    .targetCountry(TARGET_COUNTRY)
                    .status(ACTIVE)
                    .lockedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(30))
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("lockedRate must be > 0");
        }
    }

    @Nested
    @DisplayName("fromQuote()")
    class FromQuote {

        @Test
        @DisplayName("should create ACTIVE lock with 30s TTL from active quote")
        void should_createActiveLock_when_quoteIsActive() {
            var quote = activeQuote();
            var paymentId = UUID.randomUUID();
            var correlationId = UUID.randomUUID();

            var lock = FxRateLock.fromQuote(quote, paymentId, correlationId,
                    SOURCE_COUNTRY, TARGET_COUNTRY);

            var expected = FxRateLock.builder()
                    .quoteId(quote.quoteId())
                    .paymentId(paymentId)
                    .correlationId(correlationId)
                    .fromCurrency(FROM_CURRENCY)
                    .toCurrency(TO_CURRENCY)
                    .sourceAmount(quote.sourceAmount())
                    .targetAmount(quote.targetAmount())
                    .lockedRate(quote.rate())
                    .feeBps(quote.feeBps())
                    .feeAmount(quote.feeAmount())
                    .sourceCountry(SOURCE_COUNTRY)
                    .targetCountry(TARGET_COUNTRY)
                    .status(ACTIVE)
                    .build();

            assertThat(lock)
                    .usingRecursiveComparison()
                    .ignoringFields("lockId", "lockedAt", "expiresAt", "consumedAt")
                    .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("should throw IllegalStateException when quote is not active")
        void should_throwIllegalStateException_when_quoteIsNotActive() {
            var expiredQuote = activeQuote().expire();
            var paymentId = UUID.randomUUID();
            var correlationId = UUID.randomUUID();

            assertThatThrownBy(() -> FxRateLock.fromQuote(expiredQuote, paymentId, correlationId,
                    SOURCE_COUNTRY, TARGET_COUNTRY))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot lock rate from non-active quote");
        }

        @Test
        @DisplayName("should throw IllegalStateException when quote is locked")
        void should_throwIllegalStateException_when_quoteIsLocked() {
            var lockedQuote = activeQuote().lock();
            var paymentId = UUID.randomUUID();
            var correlationId = UUID.randomUUID();

            assertThatThrownBy(() -> FxRateLock.fromQuote(lockedQuote, paymentId, correlationId,
                    SOURCE_COUNTRY, TARGET_COUNTRY))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot lock rate from non-active quote");
        }
    }

    @Nested
    @DisplayName("consume()")
    class Consume {

        @Test
        @DisplayName("should transition ACTIVE to CONSUMED and set consumedAt")
        void should_transitionToConsumed_when_activeWithCorrectPaymentId() {
            var lock = activeLock();

            var consumed = lock.consume(lock.paymentId());

            var expected = lock.toBuilder().status(CONSUMED).build();
            assertThat(consumed)
                    .usingRecursiveComparison()
                    .ignoringFields("consumedAt")
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when paymentId does not match")
        void should_throwIllegalArgumentException_when_wrongPaymentId() {
            var lock = activeLock();
            var wrongPaymentId = UUID.randomUUID();

            assertThatThrownBy(() -> lock.consume(wrongPaymentId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("belongs to payment")
                    .hasMessageContaining(lock.paymentId().toString());
        }

        @Test
        @DisplayName("should throw StateMachineException when consuming already CONSUMED lock")
        void should_throwStateMachineException_when_doubleConsume() {
            var lock = activeLock();
            var consumed = lock.consume(lock.paymentId());

            assertThatThrownBy(() -> consumed.consume(consumed.paymentId()))
                    .isInstanceOf(StateMachineException.class);
        }

        @Test
        @DisplayName("should throw StateMachineException when consuming EXPIRED lock")
        void should_throwStateMachineException_when_consumingExpiredLock() {
            var lock = activeLock().expire();

            assertThatThrownBy(() -> lock.consume(lock.paymentId()))
                    .isInstanceOf(StateMachineException.class);
        }

        @Test
        @DisplayName("should throw IllegalStateException when consuming time-expired lock")
        void should_throwIllegalStateException_when_consumingTimeExpiredLock() {
            var lock = expiredTimeLock();

            assertThatThrownBy(() -> lock.consume(lock.paymentId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("has expired at");
        }
    }

    @Nested
    @DisplayName("expire()")
    class Expire {

        @Test
        @DisplayName("should transition ACTIVE to EXPIRED")
        void should_transitionToExpired_when_activeLock() {
            var lock = activeLock();

            var expired = lock.expire();

            var expected = lock.toBuilder().status(EXPIRED).build();
            assertThat(expired)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("should throw StateMachineException when expiring already EXPIRED lock")
        void should_throwStateMachineException_when_expiringExpiredLock() {
            var expired = activeLock().expire();

            assertThatThrownBy(expired::expire)
                    .isInstanceOf(StateMachineException.class);
        }

        @Test
        @DisplayName("should throw StateMachineException when expiring CONSUMED lock")
        void should_throwStateMachineException_when_expiringConsumedLock() {
            var lock = activeLock();
            var consumed = lock.consume(lock.paymentId());

            assertThatThrownBy(consumed::expire)
                    .isInstanceOf(StateMachineException.class);
        }
    }

    @Nested
    @DisplayName("isActive()")
    class IsActive {

        @Test
        @DisplayName("should return true for ACTIVE lock not past expiresAt")
        void should_returnTrue_when_activeAndNotExpired() {
            var lock = activeLock();

            assertThat(lock.isActive()).isTrue();
        }

        @Test
        @DisplayName("should return false for CONSUMED lock")
        void should_returnFalse_when_consumed() {
            var lock = activeLock();
            var consumed = lock.consume(lock.paymentId());

            assertThat(consumed.isActive()).isFalse();
        }

        @Test
        @DisplayName("should return false for EXPIRED lock")
        void should_returnFalse_when_expired() {
            var expired = activeLock().expire();

            assertThat(expired.isActive()).isFalse();
        }

        @Test
        @DisplayName("should return false when past expiresAt with ACTIVE status")
        void should_returnFalse_when_timeExpired() {
            var lock = expiredTimeLock();

            assertThat(lock.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("isExpired()")
    class IsExpired {

        @Test
        @DisplayName("should return true for EXPIRED status")
        void should_returnTrue_when_statusExpired() {
            var expired = activeLock().expire();

            assertThat(expired.isExpired()).isTrue();
        }

        @Test
        @DisplayName("should return true when past expiresAt")
        void should_returnTrue_when_pastExpiresAt() {
            var lock = expiredTimeLock();

            assertThat(lock.isExpired()).isTrue();
        }

        @Test
        @DisplayName("should return false for fresh ACTIVE lock")
        void should_returnFalse_when_activeAndNotPastExpiry() {
            var lock = activeLock();

            assertThat(lock.isExpired()).isFalse();
        }
    }
}
