package com.stablecoin.payments.fx.application.controller;

import com.stablecoin.payments.fx.api.response.CorridorResponse;
import com.stablecoin.payments.fx.application.mapper.FxResponseMapper;
import com.stablecoin.payments.fx.domain.service.LiquidityPoolQueryHandler;
import com.stablecoin.payments.fx.domain.service.LiquidityPoolQueryHandler.CorridorSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static com.stablecoin.payments.fx.fixtures.CorridorRateFixtures.aUsdEurRate;
import static com.stablecoin.payments.fx.fixtures.LiquidityPoolFixtures.aGbpEurPool;
import static com.stablecoin.payments.fx.fixtures.LiquidityPoolFixtures.aUsdEurPool;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("CorridorController")
class CorridorControllerTest {

    @Mock
    private LiquidityPoolQueryHandler queryHandler;

    @Mock
    private FxResponseMapper responseMapper;

    @InjectMocks
    private CorridorController controller;

    @Test
    @DisplayName("should delegate to query handler and map each snapshot")
    void shouldListCorridors() {
        var now = Instant.now();
        var usdEurPool = aUsdEurPool();
        var gbpEurPool = aGbpEurPool();
        var usdEurRate = aUsdEurRate();
        var usdEurSnapshot = new CorridorSnapshot(usdEurPool, usdEurRate);
        var gbpEurSnapshot = new CorridorSnapshot(gbpEurPool, null);

        var usdEurResponse = new CorridorResponse(
                "USD", "EUR", new BigDecimal("0.92"), 30, 30, "REFINITIV", now);
        var gbpEurResponse = new CorridorResponse(
                "GBP", "EUR", null, 0, 0, "unavailable", null);

        given(queryHandler.listCorridors()).willReturn(List.of(usdEurSnapshot, gbpEurSnapshot));
        given(responseMapper.toResponse(usdEurSnapshot)).willReturn(usdEurResponse);
        given(responseMapper.toResponse(gbpEurSnapshot)).willReturn(gbpEurResponse);

        var result = controller.listCorridors();

        assertThat(result).hasSize(2);
        assertThat(result.get(0))
                .usingRecursiveComparison()
                .isEqualTo(usdEurResponse);
        assertThat(result.get(1))
                .usingRecursiveComparison()
                .isEqualTo(gbpEurResponse);
    }

    @Test
    @DisplayName("should return empty list when no corridors available")
    void shouldReturnEmptyList() {
        given(queryHandler.listCorridors()).willReturn(List.of());

        var result = controller.listCorridors();

        assertThat(result).isEmpty();
    }
}
