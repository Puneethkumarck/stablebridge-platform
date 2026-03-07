package com.stablecoin.payments.fx.domain.port;

import com.stablecoin.payments.fx.domain.model.CorridorRate;

import java.util.Optional;

public interface RateCache {
    void put(String fromCurrency, String toCurrency, CorridorRate rate);
    Optional<CorridorRate> get(String fromCurrency, String toCurrency);
}
