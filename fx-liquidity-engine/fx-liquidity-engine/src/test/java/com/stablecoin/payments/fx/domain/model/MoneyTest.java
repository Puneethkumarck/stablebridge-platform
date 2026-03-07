package com.stablecoin.payments.fx.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Money")
class MoneyTest {

    @Test
    @DisplayName("should create valid Money with positive amount and currency")
    void should_createMoney_when_validInputProvided() {
        var money = new Money(new BigDecimal("100.50"), "USD");

        var expected = new Money(new BigDecimal("100.50"), "USD");
        assertThat(money)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("should create valid Money with zero amount")
    void should_createMoney_when_amountIsZero() {
        var money = new Money(BigDecimal.ZERO, "EUR");

        var expected = new Money(BigDecimal.ZERO, "EUR");
        assertThat(money)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when amount is negative")
    void should_throwIllegalArgumentException_when_amountIsNegative() {
        assertThatThrownBy(() -> new Money(new BigDecimal("-1"), "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be non-negative");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when amount is null")
    void should_throwIllegalArgumentException_when_amountIsNull() {
        assertThatThrownBy(() -> new Money(null, "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be non-negative");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when currency is null")
    void should_throwIllegalArgumentException_when_currencyIsNull() {
        assertThatThrownBy(() -> new Money(BigDecimal.TEN, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Currency must not be blank");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when currency is blank")
    void should_throwIllegalArgumentException_when_currencyIsBlank() {
        assertThatThrownBy(() -> new Money(BigDecimal.TEN, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Currency must not be blank");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when currency is empty")
    void should_throwIllegalArgumentException_when_currencyIsEmpty() {
        assertThatThrownBy(() -> new Money(BigDecimal.TEN, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Currency must not be blank");
    }
}
