package com.stablecoin.payments.fx.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LiquidityPool")
class LiquidityPoolTest {

    private static final String FROM_CURRENCY = "USD";
    private static final String TO_CURRENCY = "EUR";
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("100000");
    private static final BigDecimal MINIMUM_THRESHOLD = new BigDecimal("10000");
    private static final BigDecimal MAXIMUM_CAPACITY = new BigDecimal("1000000");

    private LiquidityPool defaultPool() {
        return LiquidityPool.create(FROM_CURRENCY, TO_CURRENCY,
                INITIAL_BALANCE, MINIMUM_THRESHOLD, MAXIMUM_CAPACITY);
    }

    @Nested
    @DisplayName("compact constructor")
    class CompactConstructor {

        @Test
        @DisplayName("should throw IllegalArgumentException when available balance is negative")
        void should_throwIllegalArgumentException_when_availableBalanceNegative() {
            assertThatThrownBy(() -> LiquidityPool.builder()
                    .poolId(UUID.randomUUID())
                    .fromCurrency(FROM_CURRENCY)
                    .toCurrency(TO_CURRENCY)
                    .availableBalance(new BigDecimal("-1"))
                    .reservedBalance(BigDecimal.ZERO)
                    .minimumThreshold(MINIMUM_THRESHOLD)
                    .maximumCapacity(MAXIMUM_CAPACITY)
                    .updatedAt(Instant.now())
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Available balance must be non-negative");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when reserved balance is negative")
        void should_throwIllegalArgumentException_when_reservedBalanceNegative() {
            assertThatThrownBy(() -> LiquidityPool.builder()
                    .poolId(UUID.randomUUID())
                    .fromCurrency(FROM_CURRENCY)
                    .toCurrency(TO_CURRENCY)
                    .availableBalance(INITIAL_BALANCE)
                    .reservedBalance(new BigDecimal("-1"))
                    .minimumThreshold(MINIMUM_THRESHOLD)
                    .maximumCapacity(MAXIMUM_CAPACITY)
                    .updatedAt(Instant.now())
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Reserved balance must be non-negative");
        }
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("should create pool with correct initial state")
        void should_createPool_when_validInputProvided() {
            var pool = LiquidityPool.create(FROM_CURRENCY, TO_CURRENCY,
                    INITIAL_BALANCE, MINIMUM_THRESHOLD, MAXIMUM_CAPACITY);

            var expected = LiquidityPool.builder()
                    .fromCurrency(FROM_CURRENCY)
                    .toCurrency(TO_CURRENCY)
                    .availableBalance(INITIAL_BALANCE)
                    .reservedBalance(BigDecimal.ZERO)
                    .minimumThreshold(MINIMUM_THRESHOLD)
                    .maximumCapacity(MAXIMUM_CAPACITY)
                    .build();

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
        @DisplayName("should decrease available and increase reserved")
        void should_decreaseAvailableAndIncreaseReserved_when_reserving() {
            var pool = defaultPool();
            var amount = new BigDecimal("5000");

            var updated = pool.reserve(amount);

            var expected = pool.toBuilder()
                    .availableBalance(INITIAL_BALANCE.subtract(amount))
                    .reservedBalance(amount)
                    .build();

            assertThat(updated)
                    .usingRecursiveComparison()
                    .ignoringFields("updatedAt")
                    .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("should throw IllegalStateException when insufficient balance")
        void should_throwIllegalStateException_when_insufficientBalance() {
            var pool = defaultPool();
            var tooMuch = INITIAL_BALANCE.add(BigDecimal.ONE);

            assertThatThrownBy(() -> pool.reserve(tooMuch))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Insufficient liquidity");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when amount is zero")
        void should_throwIllegalArgumentException_when_amountIsZero() {
            var pool = defaultPool();

            assertThatThrownBy(() -> pool.reserve(BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Reserve amount must be positive");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when amount is negative")
        void should_throwIllegalArgumentException_when_amountIsNegative() {
            var pool = defaultPool();

            assertThatThrownBy(() -> pool.reserve(new BigDecimal("-100")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Reserve amount must be positive");
        }

        @Test
        @DisplayName("should update updatedAt timestamp")
        void should_updateTimestamp_when_reserving() {
            var pool = defaultPool();
            var before = Instant.now();

            var updated = pool.reserve(new BigDecimal("1000"));

            assertThat(updated.updatedAt()).isAfterOrEqualTo(before);
        }
    }

    @Nested
    @DisplayName("release()")
    class Release {

        @Test
        @DisplayName("should increase available and decrease reserved")
        void should_increaseAvailableAndDecreaseReserved_when_releasing() {
            var pool = defaultPool().reserve(new BigDecimal("5000"));
            var releaseAmount = new BigDecimal("3000");

            var updated = pool.release(releaseAmount);

            var expected = pool.toBuilder()
                    .availableBalance(pool.availableBalance().add(releaseAmount))
                    .reservedBalance(pool.reservedBalance().subtract(releaseAmount))
                    .build();

            assertThat(updated)
                    .usingRecursiveComparison()
                    .ignoringFields("updatedAt")
                    .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("should throw IllegalStateException when releasing more than reserved")
        void should_throwIllegalStateException_when_releasingMoreThanReserved() {
            var pool = defaultPool().reserve(new BigDecimal("1000"));

            assertThatThrownBy(() -> pool.release(new BigDecimal("2000")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot release more than reserved");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when amount is zero")
        void should_throwIllegalArgumentException_when_amountIsZero() {
            var pool = defaultPool();

            assertThatThrownBy(() -> pool.release(BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Release amount must be positive");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when amount is negative")
        void should_throwIllegalArgumentException_when_amountIsNegative() {
            var pool = defaultPool();

            assertThatThrownBy(() -> pool.release(new BigDecimal("-100")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Release amount must be positive");
        }
    }

    @Nested
    @DisplayName("consumeReservation()")
    class ConsumeReservation {

        @Test
        @DisplayName("should decrease reserved only, not change available")
        void should_decreaseReservedOnly_when_consuming() {
            var pool = defaultPool().reserve(new BigDecimal("5000"));
            var consumeAmount = new BigDecimal("3000");

            var updated = pool.consumeReservation(consumeAmount);

            var expected = pool.toBuilder()
                    .reservedBalance(pool.reservedBalance().subtract(consumeAmount))
                    .build();

            assertThat(updated)
                    .usingRecursiveComparison()
                    .ignoringFields("updatedAt")
                    .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("should throw IllegalStateException when consuming more than reserved")
        void should_throwIllegalStateException_when_consumingMoreThanReserved() {
            var pool = defaultPool().reserve(new BigDecimal("1000"));

            assertThatThrownBy(() -> pool.consumeReservation(new BigDecimal("2000")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot consume more than reserved");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when amount is zero")
        void should_throwIllegalArgumentException_when_amountIsZero() {
            var pool = defaultPool();

            assertThatThrownBy(() -> pool.consumeReservation(BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Consume amount must be positive");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when amount is negative")
        void should_throwIllegalArgumentException_when_amountIsNegative() {
            var pool = defaultPool();

            assertThatThrownBy(() -> pool.consumeReservation(new BigDecimal("-100")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Consume amount must be positive");
        }
    }

    @Nested
    @DisplayName("totalBalance()")
    class TotalBalance {

        @Test
        @DisplayName("should return available plus reserved")
        void should_returnSum_when_called() {
            var pool = defaultPool().reserve(new BigDecimal("5000"));

            assertThat(pool.totalBalance())
                    .isEqualByComparingTo(INITIAL_BALANCE);
        }

        @Test
        @DisplayName("should return only available when no reservation")
        void should_returnAvailable_when_noReservation() {
            var pool = defaultPool();

            assertThat(pool.totalBalance())
                    .isEqualByComparingTo(INITIAL_BALANCE);
        }
    }

    @Nested
    @DisplayName("isBelowThreshold()")
    class IsBelowThreshold {

        @Test
        @DisplayName("should return true when available is below minimum threshold")
        void should_returnTrue_when_belowThreshold() {
            // Reserve enough to bring available below threshold
            var reserveAmount = INITIAL_BALANCE.subtract(MINIMUM_THRESHOLD).add(BigDecimal.ONE);
            var pool = defaultPool().reserve(reserveAmount);

            assertThat(pool.isBelowThreshold()).isTrue();
        }

        @Test
        @DisplayName("should return false when available is above minimum threshold")
        void should_returnFalse_when_aboveThreshold() {
            var pool = defaultPool();

            assertThat(pool.isBelowThreshold()).isFalse();
        }

        @Test
        @DisplayName("should return false when available equals minimum threshold")
        void should_returnFalse_when_equalsThreshold() {
            var reserveAmount = INITIAL_BALANCE.subtract(MINIMUM_THRESHOLD);
            var pool = defaultPool().reserve(reserveAmount);

            assertThat(pool.isBelowThreshold()).isFalse();
        }

        @Test
        @DisplayName("should return false when minimum threshold is null")
        void should_returnFalse_when_thresholdIsNull() {
            var pool = LiquidityPool.create(FROM_CURRENCY, TO_CURRENCY,
                    INITIAL_BALANCE, null, MAXIMUM_CAPACITY);

            assertThat(pool.isBelowThreshold()).isFalse();
        }
    }

    @Nested
    @DisplayName("hasSufficientLiquidity()")
    class HasSufficientLiquidity {

        @Test
        @DisplayName("should return true when available >= amount")
        void should_returnTrue_when_sufficientLiquidity() {
            var pool = defaultPool();

            assertThat(pool.hasSufficientLiquidity(new BigDecimal("50000"))).isTrue();
        }

        @Test
        @DisplayName("should return true when available equals amount")
        void should_returnTrue_when_exactlyEnough() {
            var pool = defaultPool();

            assertThat(pool.hasSufficientLiquidity(INITIAL_BALANCE)).isTrue();
        }

        @Test
        @DisplayName("should return false when available < amount")
        void should_returnFalse_when_insufficientLiquidity() {
            var pool = defaultPool();

            assertThat(pool.hasSufficientLiquidity(INITIAL_BALANCE.add(BigDecimal.ONE))).isFalse();
        }
    }

    @Nested
    @DisplayName("corridor()")
    class CorridorMethod {

        @Test
        @DisplayName("should return fromCurrency:toCurrency")
        void should_returnCorridorKey_when_called() {
            var pool = defaultPool();

            assertThat(pool.corridor()).isEqualTo("USD:EUR");
        }
    }
}
