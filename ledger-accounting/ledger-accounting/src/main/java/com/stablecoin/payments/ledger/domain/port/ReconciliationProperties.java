package com.stablecoin.payments.ledger.domain.port;

import java.math.BigDecimal;

public interface ReconciliationProperties {

    BigDecimal tolerance();

    long retryIntervalMs();

    boolean retryEnabled();
}
