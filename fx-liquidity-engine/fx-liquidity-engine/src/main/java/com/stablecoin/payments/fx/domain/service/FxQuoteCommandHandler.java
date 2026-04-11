package com.stablecoin.payments.fx.domain.service;

import com.stablecoin.payments.fx.domain.exception.QuoteNotFoundException;
import com.stablecoin.payments.fx.domain.exception.RateUnavailableException;
import com.stablecoin.payments.fx.domain.model.FxQuote;
import com.stablecoin.payments.fx.domain.port.FxQuoteRepository;
import com.stablecoin.payments.fx.domain.port.RateCache;
import com.stablecoin.payments.fx.domain.port.RateProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FxQuoteCommandHandler {

    private final RateProvider rateProvider;
    private final RateCache rateCache;
    private final QuoteService quoteService;
    private final FxQuoteRepository quoteRepository;

    @Transactional
    public FxQuote getQuote(String fromCurrency, String toCurrency, BigDecimal amount) {
        log.info("Getting FX quote for {}:{} amount={}", fromCurrency, toCurrency, amount);

        var corridorRate = rateCache.get(fromCurrency, toCurrency)
                .or(() -> {
                    log.info("Cache miss for {}:{}, fetching from provider", fromCurrency, toCurrency);
                    var providerRate = rateProvider.getRate(fromCurrency, toCurrency);
                    providerRate.ifPresent(rate -> rateCache.put(fromCurrency, toCurrency, rate));
                    return providerRate;
                })
                .orElseThrow(() -> {
                    log.warn("No rate available for {}:{}", fromCurrency, toCurrency);
                    return RateUnavailableException.forCorridor(fromCurrency, toCurrency);
                });

        var quote = quoteService.createQuote(fromCurrency, toCurrency, amount, corridorRate);

        var saved = quoteRepository.save(quote);
        log.info("Quote created: quoteId={} rate={} expires={}",
                saved.quoteId(), saved.rate(), saved.expiresAt());

        return saved;
    }

    @Transactional(readOnly = true)
    public FxQuote getQuoteById(UUID quoteId) {
        return quoteRepository.findById(quoteId)
                .orElseThrow(() -> QuoteNotFoundException.withId(quoteId));
    }
}
