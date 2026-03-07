package com.stablecoin.payments.fx.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Corridor")
class CorridorTest {

    @Nested
    @DisplayName("creation")
    class Creation {

        @Test
        @DisplayName("should create valid Corridor with different currencies")
        void should_createCorridor_when_differentCurrencies() {
            var corridor = new Corridor("USD", "EUR");

            var expected = new Corridor("USD", "EUR");
            assertThat(corridor)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when fromCurrency and toCurrency are the same")
        void should_throwIllegalArgumentException_when_sameCurrencies() {
            assertThatThrownBy(() -> new Corridor("USD", "USD"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("fromCurrency and toCurrency must differ");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when fromCurrency is null")
        void should_throwIllegalArgumentException_when_fromCurrencyIsNull() {
            assertThatThrownBy(() -> new Corridor(null, "EUR"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("fromCurrency required");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when fromCurrency is blank")
        void should_throwIllegalArgumentException_when_fromCurrencyIsBlank() {
            assertThatThrownBy(() -> new Corridor("  ", "EUR"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("fromCurrency required");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when fromCurrency is empty")
        void should_throwIllegalArgumentException_when_fromCurrencyIsEmpty() {
            assertThatThrownBy(() -> new Corridor("", "EUR"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("fromCurrency required");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when toCurrency is null")
        void should_throwIllegalArgumentException_when_toCurrencyIsNull() {
            assertThatThrownBy(() -> new Corridor("USD", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("toCurrency required");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when toCurrency is blank")
        void should_throwIllegalArgumentException_when_toCurrencyIsBlank() {
            assertThatThrownBy(() -> new Corridor("USD", "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("toCurrency required");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when toCurrency is empty")
        void should_throwIllegalArgumentException_when_toCurrencyIsEmpty() {
            assertThatThrownBy(() -> new Corridor("USD", ""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("toCurrency required");
        }
    }

    @Nested
    @DisplayName("key()")
    class Key {

        @Test
        @DisplayName("should return fromCurrency:toCurrency")
        void should_returnKey_when_called() {
            var corridor = new Corridor("USD", "EUR");

            assertThat(corridor.key()).isEqualTo("USD:EUR");
        }

        @Test
        @DisplayName("should return different keys for different corridors")
        void should_returnDifferentKeys_when_differentCorridors() {
            var usdEur = new Corridor("USD", "EUR");
            var eurUsd = new Corridor("EUR", "USD");

            assertThat(usdEur.key()).isNotEqualTo(eurUsd.key());
        }
    }
}
