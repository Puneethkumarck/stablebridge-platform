package com.stablecoin.payments.fx.domain.port;

import com.stablecoin.payments.fx.domain.model.FxQuote;

import java.util.Optional;
import java.util.UUID;

public interface FxQuoteRepository {
    FxQuote save(FxQuote quote);
    Optional<FxQuote> findById(UUID quoteId);
}
