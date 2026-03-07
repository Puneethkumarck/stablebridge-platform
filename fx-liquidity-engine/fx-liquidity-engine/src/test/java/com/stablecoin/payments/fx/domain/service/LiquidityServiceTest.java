package com.stablecoin.payments.fx.domain.service;

import com.stablecoin.payments.fx.domain.model.LiquidityPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LiquidityService")
class LiquidityServiceTest {

    private LiquidityService liquidityService;

    private static final String FROM_CURRENCY = "USD";
    private static final String TO_CURRENCY = "EUR";
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("100000");
    private static final BigDecimal MINIMUM_THRESHOLD = new BigDecimal("10000");
    private static final BigDecimal MAXIMUM_CAPACITY = new BigDecimal("1000000");

    private LiquidityPool defaultPool() {
        return LiquidityPool.create(FROM_CURRENCY, TO_CURRENCY,
                INITIAL_BALANCE, MINIMUM_THRESHOLD, MAXIMUM_CAPACITY);
    }

    @BeforeEach
    void setUp() {
        liquidityService = new LiquidityService();
    }

    @Nested
    @DisplayName("createPool()")
    class CreatePool {

        @Test
        @DisplayName("should return pool with initial balance")
        void should_returnPool_when_validInputProvided() {
            var pool = liquidityService.createPool(FROM_CURRENCY, TO_CURRENCY,
                    INITIAL_BALANCE, MINIMUM_THRESHOLD, MAXIMUM_CAPACITY);

            var expected = LiquidityPool.create(FROM_CURRENCY, TO_CURRENCY,
                    INITIAL_BALANCE, MINIMUM_THRESHOLD, MAXIMUM_CAPACITY);

            assertThat(pool)
                    .usingRecursiveComparison()
                    .ignoringFields("poolId", "updatedAt")
                    .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                    .isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("reserve()")
    class Reserve {

        @Test
        @DisplayName("should return pool with updated balance")
        void should_returnUpdatedPool_when_reserving() {
            var pool = defaultPool();
            var amount = new BigDecimal("5000");

            var updated = liquidityService.reserve(pool, amount);

            var expected = pool.reserve(amount);
            assertThat(updated)
                    .usingRecursiveComparison()
                    .ignoringFields("updatedAt")
                    .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("should not throw when reserve brings pool below threshold")
        void should_notThrow_when_belowThreshold() {
            var pool = defaultPool();
            // Reserve enough to go below threshold (available = 100000, threshold = 10000)
            var amount = new BigDecimal("95000");

            var updated = liquidityService.reserve(pool, amount);

            var expected = pool.reserve(amount);
            assertThat(updated)
                    .usingRecursiveComparison()
                    .ignoringFields("updatedAt")
                    .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                    .isEqualTo(expected);
            assertThat(updated.isBelowThreshold()).isTrue();
        }
    }

    @Nested
    @DisplayName("release()")
    class Release {

        @Test
        @DisplayName("should return pool with released balance")
        void should_returnUpdatedPool_when_releasing() {
            var pool = defaultPool().reserve(new BigDecimal("5000"));
            var releaseAmount = new BigDecimal("3000");

            var updated = liquidityService.release(pool, releaseAmount);

            var expected = pool.release(releaseAmount);
            assertThat(updated)
                    .usingRecursiveComparison()
                    .ignoringFields("updatedAt")
                    .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                    .isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("consumeReservation()")
    class ConsumeReservation {

        @Test
        @DisplayName("should return pool with consumed reservation")
        void should_returnUpdatedPool_when_consuming() {
            var pool = defaultPool().reserve(new BigDecimal("5000"));
            var consumeAmount = new BigDecimal("3000");

            var updated = liquidityService.consumeReservation(pool, consumeAmount);

            var expected = pool.consumeReservation(consumeAmount);
            assertThat(updated)
                    .usingRecursiveComparison()
                    .ignoringFields("updatedAt")
                    .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                    .isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("isBelowThreshold()")
    class IsBelowThreshold {

        @Test
        @DisplayName("should return true when pool is below threshold")
        void should_returnTrue_when_belowThreshold() {
            var reserveAmount = INITIAL_BALANCE.subtract(MINIMUM_THRESHOLD).add(BigDecimal.ONE);
            var pool = defaultPool().reserve(reserveAmount);

            assertThat(liquidityService.isBelowThreshold(pool)).isTrue();
        }

        @Test
        @DisplayName("should return false when pool is above threshold")
        void should_returnFalse_when_aboveThreshold() {
            var pool = defaultPool();

            assertThat(liquidityService.isBelowThreshold(pool)).isFalse();
        }
    }
}
