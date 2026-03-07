package com.stablecoin.payments.fx.domain.service;

import com.stablecoin.payments.fx.domain.model.CorridorRate;
import com.stablecoin.payments.fx.domain.model.FxQuote;
import com.stablecoin.payments.fx.domain.model.FxRateLock;
import com.stablecoin.payments.fx.domain.model.LiquidityPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LockService")
class LockServiceTest {

    private LockService lockService;

    private static final String FROM_CURRENCY = "USD";
    private static final String TO_CURRENCY = "EUR";
    private static final String SOURCE_COUNTRY = "US";
    private static final String TARGET_COUNTRY = "DE";
    private static final BigDecimal SOURCE_AMOUNT = new BigDecimal("1000");
    private static final BigDecimal POOL_BALANCE = new BigDecimal("100000");
    private static final BigDecimal MINIMUM_THRESHOLD = new BigDecimal("10000");
    private static final BigDecimal MAXIMUM_CAPACITY = new BigDecimal("1000000");

    private FxQuote activeQuote() {
        var corridorRate = CorridorRate.builder()
                .fromCurrency(FROM_CURRENCY)
                .toCurrency(TO_CURRENCY)
                .rate(new BigDecimal("0.92"))
                .spreadBps(30)
                .feeBps(30)
                .provider("test-provider")
                .ageMs(100)
                .build();
        return FxQuote.create(FROM_CURRENCY, TO_CURRENCY, SOURCE_AMOUNT, corridorRate, 10);
    }

    private LiquidityPool defaultPool() {
        return LiquidityPool.create(FROM_CURRENCY, TO_CURRENCY,
                POOL_BALANCE, MINIMUM_THRESHOLD, MAXIMUM_CAPACITY);
    }

    @BeforeEach
    void setUp() {
        lockService = new LockService();
    }

    @Nested
    @DisplayName("lockRate()")
    class LockRate {

        @Test
        @DisplayName("should return LockResult with locked quote, lock, and updated pool")
        void should_returnLockResult_when_validInputProvided() {
            var quote = activeQuote();
            var pool = defaultPool();
            var paymentId = UUID.randomUUID();
            var correlationId = UUID.randomUUID();

            var result = lockService.lockRate(quote, paymentId, correlationId,
                    SOURCE_COUNTRY, TARGET_COUNTRY, pool);

            // Verify locked quote
            var expectedQuote = quote.lock();
            assertThat(result.lockedQuote())
                    .usingRecursiveComparison()
                    .isEqualTo(expectedQuote);

            // Verify lock
            var expectedLock = FxRateLock.fromQuote(quote, paymentId, correlationId,
                    SOURCE_COUNTRY, TARGET_COUNTRY);
            assertThat(result.lock())
                    .usingRecursiveComparison()
                    .ignoringFields("lockId", "lockedAt", "expiresAt", "consumedAt")
                    .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                    .isEqualTo(expectedLock);

            // Verify pool
            var expectedPool = pool.reserve(quote.targetAmount());
            assertThat(result.updatedPool())
                    .usingRecursiveComparison()
                    .ignoringFields("updatedAt")
                    .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                    .isEqualTo(expectedPool);
        }

        @Test
        @DisplayName("should throw IllegalStateException when quote is not active")
        void should_throwIllegalStateException_when_quoteIsNotActive() {
            var expiredQuote = activeQuote().expire();
            var pool = defaultPool();
            var paymentId = UUID.randomUUID();
            var correlationId = UUID.randomUUID();

            assertThatThrownBy(() -> lockService.lockRate(expiredQuote, paymentId, correlationId,
                    SOURCE_COUNTRY, TARGET_COUNTRY, pool))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("is not active");
        }

        @Test
        @DisplayName("should throw IllegalStateException when pool has insufficient liquidity")
        void should_throwIllegalStateException_when_insufficientLiquidity() {
            var quote = activeQuote();
            var smallPool = LiquidityPool.create(FROM_CURRENCY, TO_CURRENCY,
                    new BigDecimal("1"), MINIMUM_THRESHOLD, MAXIMUM_CAPACITY);
            var paymentId = UUID.randomUUID();
            var correlationId = UUID.randomUUID();

            assertThatThrownBy(() -> lockService.lockRate(quote, paymentId, correlationId,
                    SOURCE_COUNTRY, TARGET_COUNTRY, smallPool))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Insufficient liquidity");
        }
    }

    @Nested
    @DisplayName("consumeLock()")
    class ConsumeLock {

        @Test
        @DisplayName("should return consumed lock")
        void should_returnConsumedLock_when_validPaymentId() {
            var quote = activeQuote();
            var paymentId = UUID.randomUUID();
            var correlationId = UUID.randomUUID();
            var lock = FxRateLock.fromQuote(quote, paymentId, correlationId,
                    SOURCE_COUNTRY, TARGET_COUNTRY);

            var consumed = lockService.consumeLock(lock, paymentId);

            var expected = lock.consume(paymentId);
            assertThat(consumed)
                    .usingRecursiveComparison()
                    .ignoringFields("consumedAt")
                    .isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("expireLock()")
    class ExpireLock {

        @Test
        @DisplayName("should return expired lock")
        void should_returnExpiredLock_when_activeLock() {
            var quote = activeQuote();
            var paymentId = UUID.randomUUID();
            var correlationId = UUID.randomUUID();
            var lock = FxRateLock.fromQuote(quote, paymentId, correlationId,
                    SOURCE_COUNTRY, TARGET_COUNTRY);

            var expired = lockService.expireLock(lock);

            var expected = lock.expire();
            assertThat(expired)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }
    }
}
