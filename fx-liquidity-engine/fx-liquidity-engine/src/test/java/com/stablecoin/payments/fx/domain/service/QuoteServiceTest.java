package com.stablecoin.payments.fx.domain.service;

import com.stablecoin.payments.fx.domain.model.CorridorRate;
import com.stablecoin.payments.fx.domain.model.FxQuote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("QuoteService")
class QuoteServiceTest {

    private QuoteService quoteService;

    private static final String FROM_CURRENCY = "USD";
    private static final String TO_CURRENCY = "EUR";
    private static final BigDecimal SOURCE_AMOUNT = new BigDecimal("1000");

    private CorridorRate defaultCorridorRate() {
        return CorridorRate.builder()
                .fromCurrency(FROM_CURRENCY)
                .toCurrency(TO_CURRENCY)
                .rate(new BigDecimal("0.92"))
                .spreadBps(30)
                .feeBps(30)
                .provider("test-provider")
                .ageMs(100)
                .build();
    }

    @BeforeEach
    void setUp() {
        quoteService = new QuoteService();
    }

    @Nested
    @DisplayName("createQuote()")
    class CreateQuote {

        @Test
        @DisplayName("should return valid ACTIVE quote")
        void should_returnActiveQuote_when_validInputProvided() {
            var corridorRate = defaultCorridorRate();

            var quote = quoteService.createQuote(FROM_CURRENCY, TO_CURRENCY,
                    SOURCE_AMOUNT, corridorRate);

            var expected = FxQuote.create(FROM_CURRENCY, TO_CURRENCY,
                    SOURCE_AMOUNT, corridorRate, 10);

            assertThat(quote)
                    .usingRecursiveComparison()
                    .ignoringFields("quoteId", "createdAt", "expiresAt")
                    .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when source amount is zero")
        void should_throwIllegalArgumentException_when_sourceAmountIsZero() {
            var corridorRate = defaultCorridorRate();

            assertThatThrownBy(() -> quoteService.createQuote(FROM_CURRENCY, TO_CURRENCY,
                    BigDecimal.ZERO, corridorRate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Source amount must be positive");
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when source amount is negative")
        void should_throwIllegalArgumentException_when_sourceAmountIsNegative() {
            var corridorRate = defaultCorridorRate();

            assertThatThrownBy(() -> quoteService.createQuote(FROM_CURRENCY, TO_CURRENCY,
                    new BigDecimal("-100"), corridorRate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Source amount must be positive");
        }
    }

    @Nested
    @DisplayName("expireQuote()")
    class ExpireQuote {

        @Test
        @DisplayName("should return expired quote")
        void should_returnExpiredQuote_when_activeQuoteProvided() {
            var corridorRate = defaultCorridorRate();
            var quote = quoteService.createQuote(FROM_CURRENCY, TO_CURRENCY,
                    SOURCE_AMOUNT, corridorRate);

            var expired = quoteService.expireQuote(quote);

            var expected = quote.expire();
            assertThat(expired)
                    .usingRecursiveComparison()
                    .isEqualTo(expected);
        }
    }
}
