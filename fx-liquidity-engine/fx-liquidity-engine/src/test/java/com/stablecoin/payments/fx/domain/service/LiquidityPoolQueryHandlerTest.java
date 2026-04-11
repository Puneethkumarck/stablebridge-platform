package com.stablecoin.payments.fx.domain.service;

import com.stablecoin.payments.fx.domain.exception.PoolNotFoundException;
import com.stablecoin.payments.fx.domain.port.LiquidityPoolRepository;
import com.stablecoin.payments.fx.domain.port.RateCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.stablecoin.payments.fx.fixtures.CorridorRateFixtures.aUsdEurRate;
import static com.stablecoin.payments.fx.fixtures.LiquidityPoolFixtures.aGbpEurPool;
import static com.stablecoin.payments.fx.fixtures.LiquidityPoolFixtures.aUsdEurPool;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("LiquidityPoolQueryHandler")
class LiquidityPoolQueryHandlerTest {

    @Mock
    private LiquidityPoolRepository poolRepository;

    @Mock
    private RateCache rateCache;

    @InjectMocks
    private LiquidityPoolQueryHandler handler;

    @Nested
    @DisplayName("listPools")
    class ListPools {

        @Test
        void shouldReturnAllPools() {
            var pool1 = aUsdEurPool();
            var pool2 = aGbpEurPool();
            given(poolRepository.findAll()).willReturn(List.of(pool1, pool2));

            var result = handler.listPools();

            assertThat(result).hasSize(2);
            assertThat(result.get(0))
                    .usingRecursiveComparison()
                    .isEqualTo(pool1);
            assertThat(result.get(1))
                    .usingRecursiveComparison()
                    .isEqualTo(pool2);
        }

        @Test
        void shouldReturnEmptyListWhenNoPools() {
            given(poolRepository.findAll()).willReturn(List.of());

            var result = handler.listPools();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getPool")
    class GetPool {

        @Test
        void shouldReturnPoolById() {
            var pool = aUsdEurPool();
            var poolId = pool.poolId();
            given(poolRepository.findById(poolId)).willReturn(Optional.of(pool));

            var result = handler.getPool(poolId);

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(pool);
        }

        @Test
        void shouldThrowWhenPoolNotFound() {
            var poolId = UUID.randomUUID();
            given(poolRepository.findById(poolId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> handler.getPool(poolId))
                    .isInstanceOf(PoolNotFoundException.class)
                    .hasMessageContaining(poolId.toString());
        }
    }

    @Nested
    @DisplayName("listCorridors")
    class ListCorridors {

        @Test
        void shouldReturnSnapshotsWithRates() {
            var pool = aUsdEurPool();
            var rate = aUsdEurRate();
            given(poolRepository.findAll()).willReturn(List.of(pool));
            given(rateCache.get("USD", "EUR")).willReturn(Optional.of(rate));

            var result = handler.listCorridors();

            assertThat(result).hasSize(1);
            var snapshot = result.getFirst();
            assertThat(snapshot.pool()).isEqualTo(pool);
            assertThat(snapshot.rate()).isEqualTo(rate);
        }

        @Test
        void shouldReturnSnapshotWithNullRateWhenCacheMisses() {
            var pool = aUsdEurPool();
            given(poolRepository.findAll()).willReturn(List.of(pool));
            given(rateCache.get("USD", "EUR")).willReturn(Optional.empty());

            var result = handler.listCorridors();

            assertThat(result).hasSize(1);
            var snapshot = result.getFirst();
            assertThat(snapshot.pool()).isEqualTo(pool);
            assertThat(snapshot.rate()).isNull();
        }
    }
}
