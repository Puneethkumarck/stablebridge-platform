package com.stablecoin.payments.fx.infrastructure.provider.frankfurter;

import java.math.BigDecimal;
import java.util.Map;

record FrankfurterRateResponse(
        BigDecimal amount,
        String base,
        String date,
        Map<String, BigDecimal> rates
) {}
