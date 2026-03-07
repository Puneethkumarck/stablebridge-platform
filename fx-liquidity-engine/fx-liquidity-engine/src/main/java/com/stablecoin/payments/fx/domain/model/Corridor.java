package com.stablecoin.payments.fx.domain.model;

public record Corridor(String fromCurrency, String toCurrency) {

    public Corridor {
        if (fromCurrency == null || fromCurrency.isBlank()) {
            throw new IllegalArgumentException("fromCurrency required");
        }
        if (toCurrency == null || toCurrency.isBlank()) {
            throw new IllegalArgumentException("toCurrency required");
        }
        if (fromCurrency.equals(toCurrency)) {
            throw new IllegalArgumentException("fromCurrency and toCurrency must differ");
        }
    }

    public String key() {
        return fromCurrency + ":" + toCurrency;
    }
}
