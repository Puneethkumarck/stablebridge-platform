package com.stablecoin.payments.fx.domain.model;

import com.stablecoin.payments.fx.domain.statemachine.StateMachineException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import static com.stablecoin.payments.fx.domain.model.FxQuoteStatus.ACTIVE;
import static com.stablecoin.payments.fx.domain.model.FxQuoteStatus.EXPIRED;
import static com.stablecoin.payments.fx.domain.model.FxQuoteStatus.LOCKED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FxQuote")
class FxQuoteTest {

    private static final String FROM_CURRENCY = "USD";
    private static final String TO_CURRENCY = "EUR";
    private static final BigDecimal SOURCE_AMOUNT = new BigDecimal("1000");
    private static final BigDecimal RATE = new BigDecimal("0.92");
    private static final int SPREAD_BPS = 30;
    private static final int FEE_BPS = 30;
    private static final String PROVIDER = "test-provider";
    private static final int QUOTE_TTL_SECONDS = 10;

    private CorridorRate defaultCorridorRate() {
        return CorridorRate.builder()
                .fromCurrency(FROM_CURRENCY)
                .toCurrency(TO_CURRENCY)
                .rate(RATE)
                .spreadBps(SPREAD_BPS)
                .feeBps(FEE_BPS)
                .provider(PROVIDER)
                .ageMs(100)
                .build();
    }

    private FxQuote activeQuote() {
        return FxQuote.create(FROM_CURRENCY, TO_CURRENCY, SOURCE_AMOUNT,
                defaultCorridorRate(), QUOTE_TTL_SECONDS);
    }

    private FxQuote expiredQuote() {
        return FxQuote.builder()
                .quoteId(UUID.randomUUID())
                .fromCurrency(FROM_CURRENCY)
                .toCurrency(TO_CURRENCY)
                .sourceAmount(SOURCE_AMOUNT)
                .targetAmount(new BigDecimal("920"))
                .rate(new BigDecimal("0.92"))
                .inverseRate(new BigDecimal("1.0869565217"))
                .spreadBps(SPREAD_BPS)
                .feeBps(FEE_BPS)
                .feeAmount(new BigDecimal("3"))
                .provider(PROVIDER)
                .status(ACTIVE)
                .createdAt(Instant.now().minusSeconds(20))
                .expiresAt(Instant.now().minusSeconds(10))
                .build();
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("should create ACTIVE quote with correct fields")
        void should_createActiveQuote_when_validInputProvided() {
            var corridorRate = defaultCorridorRate();

            var quote = FxQuote.create(FROM_CURRENCY, TO_CURRENCY, SOURCE_AMOUNT,
                    corridorRate, QUOTE_TTL_SECONDS);

            var spreadFactor = BigDecimal.ONE.subtract(BigDecimal.valueOf(SPREAD_BPS).movePointLeft(4));
            var expectedRate = RATE.multiply(spreadFactor);
            var expectedTarget = SOURCE_AMOUNT.multiply(expectedRate);
            var expectedFee = SOURCE_AMOUNT.multiply(BigDecimal.valueOf(FEE_BPS).movePointLeft(4));
            var expectedInverse = BigDecimal.ONE.divide(expectedRate, 10, RoundingMode.HALF_UP);

            var expected = FxQuote.builder()
                    .fromCurrency(FROM_CURRENCY)
                    .toCurrency(TO_CURRENCY)
                    .sourceAmount(SOURCE_AMOUNT)
                    .targetAmount(expectedTarget)
                    .rate(expectedRate)
                    .inverseRate(expectedInverse)
                    .spreadBps(SPREAD_BPS)
                    .feeBps(FEE_BPS)
                    .feeAmount(expectedFee)
                    .provider(PROVIDER)
                    .status(ACTIVE)
                    .build();

            assertThat(quote)
                    .usingRecursiveComparison()
                    .ignoringFields("quoteId", "createdAt", "expiresAt")
                    .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("should compute effective rate with spread applied")
        void should_computeEffectiveRate_when_spreadApplied() {
            var corridorRate = defaultCorridorRate();

            var quote = FxQuote.create(FROM_CURRENCY, TO_CURRENCY, SOURCE_AMOUNT,
                    corridorRate, QUOTE_TTL_SECONDS);

            // spreadFactor = 1 - (30 / 10000) = 0.9970
            var spreadFactor = BigDecimal.ONE.subtract(BigDecimal.valueOf(SPREAD_BPS).movePointLeft(4));
            var expectedRate = RATE.multiply(spreadFactor);
            assertThat(quote.rate()).isEqualByComparingTo(expectedRate);
        }

        @Test
        @DisplayName("should compute target amount from source and effective rate")
        void should_computeTargetAmount_when_created() {
            var corridorRate = defaultCorridorRate();

            var quote = FxQuote.create(FROM_CURRENCY, TO_CURRENCY, SOURCE_AMOUNT,
                    corridorRate, QUOTE_TTL_SECONDS);

            var expectedTarget = SOURCE_AMOUNT.multiply(quote.rate());
            assertThat(quote.targetAmount()).isEqualByComparingTo(expectedTarget);
        }

        @Test
        @DisplayName("should compute fee amount from source amount and feeBps")
        void should_computeFeeAmount_when_created() {
            var corridorRate = defaultCorridorRate();

            var quote = FxQuote.create(FROM_CURRENCY, TO_CURRENCY, SOURCE_AMOUNT,
                    corridorRate, QUOTE_TTL_SECONDS);

            var expectedFee = SOURCE_AMOUNT.multiply(BigDecimal.valueOf(FEE_BPS).movePointLeft(4));
            assertThat(quote.feeAmount()).isEqualByComparingTo(expectedFee);
        }

        @Test
        @DisplayName("should compute inverse rate")
        void should_computeInverseRate_when_created() {
            var corridorRate = defaultCorridorRate();

            var quote = FxQuote.create(FROM_CURRENCY, TO_CURRENCY, SOURCE_AMOUNT,
                    corridorRate, QUOTE_TTL_SECONDS);

            var expectedInverse = BigDecimal.ONE.divide(quote.rate(), 10, RoundingMode.HALF_UP);
            assertThat(quote.inverseRate()).isEqualByComparingTo(expectedInverse);
        }

        @Test
        @DisplayName("should set expiresAt to createdAt plus TTL seconds")
        void should_setExpiresAt_when_ttlProvided() {
            var quote = FxQuote.create(FROM_CURRENCY, TO_CURRENCY, SOURCE_AMOUNT,
                    defaultCorridorRate(), QUOTE_TTL_SECONDS);

            assertThat(quote.expiresAt()).isEqualTo(quote.createdAt().plusSeconds(QUOTE_TTL_SECONDS));
        }
    }

    @Nested
    @DisplayName("lock()")
    class Lock {

        @Test
        @DisplayName("should transition ACTIVE to LOCKED")
        void should_transitionToLocked_when_activeQuote() {
            var quote = activeQuote();

            var locked = quote.lock();

            var expected = quote.toBuilder().status(LOCKED).build();
            assertThat(locked)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("should throw StateMachineException when locking LOCKED quote")
        void should_throwStateMachineException_when_lockingLockedQuote() {
            var locked = activeQuote().lock();

            assertThatThrownBy(locked::lock)
                    .isInstanceOf(StateMachineException.class);
        }

        @Test
        @DisplayName("should throw StateMachineException when locking EXPIRED quote")
        void should_throwStateMachineException_when_lockingExpiredQuote() {
            var expired = activeQuote().expire();

            assertThatThrownBy(expired::lock)
                    .isInstanceOf(StateMachineException.class);
        }

        @Test
        @DisplayName("should throw IllegalStateException when locking time-expired quote")
        void should_throwIllegalStateException_when_lockingTimeExpiredQuote() {
            var quote = expiredQuote();

            assertThatThrownBy(quote::lock)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("has expired at");
        }
    }

    @Nested
    @DisplayName("expire()")
    class Expire {

        @Test
        @DisplayName("should transition ACTIVE to EXPIRED")
        void should_transitionToExpired_when_activeQuote() {
            var quote = activeQuote();

            var expired = quote.expire();

            var expected = quote.toBuilder().status(EXPIRED).build();
            assertThat(expired)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("should throw StateMachineException when expiring already EXPIRED quote")
        void should_throwStateMachineException_when_expiringExpiredQuote() {
            var expired = activeQuote().expire();

            assertThatThrownBy(expired::expire)
                    .isInstanceOf(StateMachineException.class);
        }

        @Test
        @DisplayName("should throw StateMachineException when expiring LOCKED quote")
        void should_throwStateMachineException_when_expiringLockedQuote() {
            var locked = activeQuote().lock();

            assertThatThrownBy(locked::expire)
                    .isInstanceOf(StateMachineException.class);
        }
    }

    @Nested
    @DisplayName("isActive()")
    class IsActive {

        @Test
        @DisplayName("should return true for ACTIVE status and not past expiresAt")
        void should_returnTrue_when_activeAndNotExpired() {
            var quote = activeQuote();

            assertThat(quote.isActive()).isTrue();
        }

        @Test
        @DisplayName("should return false for LOCKED status")
        void should_returnFalse_when_locked() {
            var locked = activeQuote().lock();

            assertThat(locked.isActive()).isFalse();
        }

        @Test
        @DisplayName("should return false for EXPIRED status")
        void should_returnFalse_when_expired() {
            var expired = activeQuote().expire();

            assertThat(expired.isActive()).isFalse();
        }

        @Test
        @DisplayName("should return false when past expiresAt even with ACTIVE status")
        void should_returnFalse_when_timeExpired() {
            var quote = expiredQuote();

            assertThat(quote.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("isExpired()")
    class IsExpired {

        @Test
        @DisplayName("should return true for EXPIRED status")
        void should_returnTrue_when_statusExpired() {
            var expired = activeQuote().expire();

            assertThat(expired.isExpired()).isTrue();
        }

        @Test
        @DisplayName("should return true when past expiresAt with ACTIVE status")
        void should_returnTrue_when_pastExpiresAt() {
            var quote = expiredQuote();

            assertThat(quote.isExpired()).isTrue();
        }

        @Test
        @DisplayName("should return false for fresh ACTIVE quote")
        void should_returnFalse_when_activeAndNotPastExpiry() {
            var quote = activeQuote();

            assertThat(quote.isExpired()).isFalse();
        }
    }
}
