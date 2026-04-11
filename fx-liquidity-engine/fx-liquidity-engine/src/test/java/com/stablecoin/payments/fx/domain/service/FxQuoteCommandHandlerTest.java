package com.stablecoin.payments.fx.domain.service;

import com.stablecoin.payments.fx.domain.exception.QuoteNotFoundException;
import com.stablecoin.payments.fx.domain.exception.RateUnavailableException;
import com.stablecoin.payments.fx.domain.port.FxQuoteRepository;
import com.stablecoin.payments.fx.domain.port.RateCache;
import com.stablecoin.payments.fx.domain.port.RateProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static com.stablecoin.payments.fx.fixtures.CorridorRateFixtures.aUsdEurRate;
import static com.stablecoin.payments.fx.fixtures.FxQuoteFixtures.anActiveQuote;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("FxQuoteCommandHandler")
class FxQuoteCommandHandlerTest {

    @Mock
    private RateProvider rateProvider;

    @Mock
    private RateCache rateCache;

    @Mock
    private QuoteService quoteService;

    @Mock
    private FxQuoteRepository quoteRepository;

    @InjectMocks
    private FxQuoteCommandHandler handler;

    @Nested
    @DisplayName("getQuote")
    class GetQuote {

        @Test
        void shouldCreateQuoteFromCachedRate() {
            var amount = new BigDecimal("10000.00");
            var corridorRate = aUsdEurRate();
            var quote = anActiveQuote();

            given(rateCache.get("USD", "EUR")).willReturn(Optional.of(corridorRate));
            given(quoteService.createQuote("USD", "EUR", amount, corridorRate)).willReturn(quote);
            given(quoteRepository.save(quote)).willReturn(quote);

            var result = handler.getQuote("USD", "EUR", amount);

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(quote);

            then(rateProvider).shouldHaveNoInteractions();
        }

        @Test
        void shouldCreateQuoteFromProviderWhenCacheMisses() {
            var amount = new BigDecimal("10000.00");
            var corridorRate = aUsdEurRate();
            var quote = anActiveQuote();

            given(rateCache.get("USD", "EUR")).willReturn(Optional.empty());
            given(rateProvider.getRate("USD", "EUR")).willReturn(Optional.of(corridorRate));
            given(quoteService.createQuote("USD", "EUR", amount, corridorRate)).willReturn(quote);
            given(quoteRepository.save(quote)).willReturn(quote);

            var result = handler.getQuote("USD", "EUR", amount);

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(quote);

            then(rateCache).should().put("USD", "EUR", corridorRate);
        }

        @Test
        void shouldThrowWhenNoRateAvailable() {
            var amount = new BigDecimal("10000.00");
            given(rateCache.get("USD", "EUR")).willReturn(Optional.empty());
            given(rateProvider.getRate("USD", "EUR")).willReturn(Optional.empty());

            assertThatThrownBy(() -> handler.getQuote("USD", "EUR", amount))
                    .isInstanceOf(RateUnavailableException.class)
                    .hasMessageContaining("USD:EUR");
        }
    }

    @Nested
    @DisplayName("getQuoteById")
    class GetQuoteById {

        @Test
        void shouldReturnQuoteById() {
            var quoteId = UUID.randomUUID();
            var quote = anActiveQuote();

            given(quoteRepository.findById(quoteId)).willReturn(Optional.of(quote));

            var result = handler.getQuoteById(quoteId);

            assertThat(result)
                    .usingRecursiveComparison()
                    .isEqualTo(quote);
        }

        @Test
        void shouldThrowWhenQuoteNotFound() {
            var quoteId = UUID.randomUUID();
            given(quoteRepository.findById(quoteId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> handler.getQuoteById(quoteId))
                    .isInstanceOf(QuoteNotFoundException.class)
                    .hasMessageContaining(quoteId.toString());
        }
    }
}
