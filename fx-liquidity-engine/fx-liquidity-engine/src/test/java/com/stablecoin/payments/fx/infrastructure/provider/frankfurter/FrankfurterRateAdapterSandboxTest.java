package com.stablecoin.payments.fx.infrastructure.provider.frankfurter;

import com.stablecoin.payments.fx.domain.model.CorridorRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sandbox tests that run against the real Frankfurter API (api.frankfurter.app).
 * <p>
 * Frankfurter is free and keyless — no env vars required. Uses ECB reference rates.
 * These tests are excluded from CI by the {@code @Tag("sandbox")} annotation.
 * <p>
 * Run manually:
 * <pre>
 * ./gradlew :fx-liquidity-engine:fx-liquidity-engine:test --tests '*FrankfurterRateAdapterSandboxTest*' -Dsandbox=true
 * </pre>
 *
 * @see <a href="https://www.frankfurter.app/docs/">Frankfurter API Docs</a>
 */
@Tag("sandbox")
@DisplayName("Frankfurter Rate Adapter Sandbox (live API)")
class FrankfurterRateAdapterSandboxTest {

    private FrankfurterRateAdapter adapter;

    @BeforeEach
    void setUp() {
        var properties = new FrankfurterProperties("https://api.frankfurter.app", 10000);
        adapter = new FrankfurterRateAdapter(properties);
    }

    @Nested
    @DisplayName("getRate")
    class GetRate {

        @Test
        @DisplayName("should fetch live USD to EUR rate from Frankfurter")
        void shouldFetchLiveUsdToEurRate() {
            var result = adapter.getRate("USD", "EUR");

            assertThat(result).isPresent();
            var rate = result.get();
            var expected = CorridorRate.builder()
                    .fromCurrency("USD")
                    .toCurrency("EUR")
                    .rate(rate.rate())
                    .spreadBps(30)
                    .feeBps(30)
                    .provider("frankfurter")
                    .ageMs(0)
                    .build();
            assertThat(rate)
                    .usingRecursiveComparison()
                    .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                    .isEqualTo(expected);
            assertThat(rate.rate()).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should fetch live EUR to GBP rate from Frankfurter")
        void shouldFetchLiveEurToGbpRate() {
            var result = adapter.getRate("EUR", "GBP");

            assertThat(result).isPresent();
            var rate = result.get();
            var expected = CorridorRate.builder()
                    .fromCurrency("EUR")
                    .toCurrency("GBP")
                    .rate(rate.rate())
                    .spreadBps(30)
                    .feeBps(30)
                    .provider("frankfurter")
                    .ageMs(0)
                    .build();
            assertThat(rate)
                    .usingRecursiveComparison()
                    .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                    .isEqualTo(expected);
            assertThat(rate.rate()).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should throw on invalid currency pair (USD to USD returns 422)")
        void shouldThrowOnSameCurrencyPair() {
            assertThatThrownBy(() -> adapter.getRate("USD", "USD"))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("providerName")
    class ProviderName {

        @Test
        @DisplayName("should return frankfurter")
        void shouldReturnProviderName() {
            assertThat(adapter.providerName()).isEqualTo("frankfurter");
        }
    }
}
