package com.stablecoin.payments.fx.application.controller;

import com.stablecoin.payments.fx.api.response.LiquidityPoolResponse;
import com.stablecoin.payments.fx.application.mapper.FxResponseMapper;
import com.stablecoin.payments.fx.domain.exception.PoolNotFoundException;
import com.stablecoin.payments.fx.domain.service.LiquidityPoolQueryHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.stablecoin.payments.fx.fixtures.LiquidityPoolFixtures.aGbpEurPool;
import static com.stablecoin.payments.fx.fixtures.LiquidityPoolFixtures.aUsdEurPool;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("LiquidityPoolController")
class LiquidityPoolControllerTest {

    @Mock
    private LiquidityPoolQueryHandler queryHandler;

    @Mock
    private FxResponseMapper responseMapper;

    @InjectMocks
    private LiquidityPoolController controller;

    @Nested
    @DisplayName("GET /v1/liquidity/pools")
    class ListPools {

        @Test
        @DisplayName("should delegate to query handler and map each pool to response")
        void shouldListPools() {
            var pool1 = aUsdEurPool();
            var pool2 = aGbpEurPool();
            var now = Instant.now();
            var response1 = new LiquidityPoolResponse(
                    pool1.poolId(), "USD", "EUR",
                    new BigDecimal("1000000.00"), BigDecimal.ZERO,
                    new BigDecimal("100000.00"), new BigDecimal("5000000.00"),
                    BigDecimal.ZERO, "HEALTHY", now);
            var response2 = new LiquidityPoolResponse(
                    pool2.poolId(), "GBP", "EUR",
                    new BigDecimal("500000.00"), BigDecimal.ZERO,
                    new BigDecimal("50000.00"), new BigDecimal("2000000.00"),
                    BigDecimal.ZERO, "HEALTHY", now);

            given(queryHandler.listPools()).willReturn(List.of(pool1, pool2));
            given(responseMapper.toResponse(pool1)).willReturn(response1);
            given(responseMapper.toResponse(pool2)).willReturn(response2);

            var result = controller.listPools();

            assertThat(result).hasSize(2);
            assertThat(result.get(0))
                    .usingRecursiveComparison()
                    .isEqualTo(response1);
            assertThat(result.get(1))
                    .usingRecursiveComparison()
                    .isEqualTo(response2);
        }

        @Test
        @DisplayName("should return empty list when no pools exist")
        void shouldReturnEmptyList() {
            given(queryHandler.listPools()).willReturn(List.of());

            var result = controller.listPools();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("GET /v1/liquidity/pools/{poolId}")
    class GetPool {

        @Test
        @DisplayName("should delegate to query handler and map response")
        void shouldGetPool() {
            var pool = aUsdEurPool();
            var poolId = pool.poolId();
            var now = Instant.now();
            var expectedResponse = new LiquidityPoolResponse(
                    poolId, "USD", "EUR",
                    new BigDecimal("1000000.00"), BigDecimal.ZERO,
                    new BigDecimal("100000.00"), new BigDecimal("5000000.00"),
                    BigDecimal.ZERO, "HEALTHY", now);

            given(queryHandler.getPool(poolId)).willReturn(pool);
            given(responseMapper.toResponse(pool)).willReturn(expectedResponse);

            var result = controller.getPool(poolId);

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(expectedResponse);

            then(queryHandler).should().getPool(poolId);
            then(responseMapper).should().toResponse(pool);
        }

        @Test
        @DisplayName("should propagate PoolNotFoundException")
        void shouldPropagatePoolNotFound() {
            var poolId = UUID.randomUUID();
            given(queryHandler.getPool(poolId))
                    .willThrow(PoolNotFoundException.withId(poolId));

            assertThatThrownBy(() -> controller.getPool(poolId))
                    .isInstanceOf(PoolNotFoundException.class)
                    .hasMessageContaining(poolId.toString());
        }
    }
}
