package com.stablecoin.payments.fx.domain.port;

import com.stablecoin.payments.fx.domain.model.CorridorRate;

import java.util.Optional;

public interface RateProvider {
    Optional<CorridorRate> getRate(String fromCurrency, String toCurrency);
    String providerName();
}
