package com.stablecoin.payments.fx.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CorridorRate")
class CorridorRateTest {

    @Test
    @DisplayName("should create valid CorridorRate when age is under 5000ms")
    void should_createCorridorRate_when_ageIsUnder5000ms() {
        var rate = CorridorRate.builder()
                .fromCurrency("USD")
                .toCurrency("EUR")
                .rate(new BigDecimal("0.92"))
                .spreadBps(30)
                .feeBps(30)
                .provider("test")
                .ageMs(100)
                .build();

        var expected = CorridorRate.builder()
                .fromCurrency("USD")
                .toCurrency("EUR")
                .rate(new BigDecimal("0.92"))
                .spreadBps(30)
                .feeBps(30)
                .provider("test")
                .ageMs(100)
                .build();

        assertThat(rate)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("should create valid CorridorRate when age is 0ms")
    void should_createCorridorRate_when_ageIsZero() {
        var rate = CorridorRate.builder()
                .fromCurrency("USD")
                .toCurrency("EUR")
                .rate(new BigDecimal("0.92"))
                .spreadBps(30)
                .feeBps(30)
                .provider("test")
                .ageMs(0)
                .build();

        assertThat(rate.ageMs()).isZero();
    }

    @Test
    @DisplayName("should create valid CorridorRate when age is 4999ms")
    void should_createCorridorRate_when_ageIsJustUnderLimit() {
        var rate = CorridorRate.builder()
                .fromCurrency("USD")
                .toCurrency("EUR")
                .rate(new BigDecimal("0.92"))
                .spreadBps(30)
                .feeBps(30)
                .provider("test")
                .ageMs(4999)
                .build();

        assertThat(rate.ageMs()).isEqualTo(4999);
    }

    @Test
    @DisplayName("should create valid CorridorRate when age is exactly 5000ms")
    void should_createCorridorRate_when_ageIsExactly5000ms() {
        var rate = CorridorRate.builder()
                .fromCurrency("USD")
                .toCurrency("EUR")
                .rate(new BigDecimal("0.92"))
                .spreadBps(30)
                .feeBps(30)
                .provider("test")
                .ageMs(5000)
                .build();

        assertThat(rate.ageMs()).isEqualTo(5000);
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when age exceeds 5000ms")
    void should_throwIllegalArgumentException_when_ageExceeds5000ms() {
        assertThatThrownBy(() -> CorridorRate.builder()
                .fromCurrency("USD")
                .toCurrency("EUR")
                .rate(new BigDecimal("0.92"))
                .spreadBps(30)
                .feeBps(30)
                .provider("test")
                .ageMs(5001)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rate must be less than 5 seconds old")
                .hasMessageContaining("5001");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when age is well over 5000ms")
    void should_throwIllegalArgumentException_when_ageIsWellOver5000ms() {
        assertThatThrownBy(() -> CorridorRate.builder()
                .fromCurrency("USD")
                .toCurrency("EUR")
                .rate(new BigDecimal("0.92"))
                .spreadBps(30)
                .feeBps(30)
                .provider("test")
                .ageMs(10000)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rate must be less than 5 seconds old");
    }
}
