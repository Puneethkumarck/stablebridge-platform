package com.stablecoin.payments.custody.domain.port;

import com.stablecoin.payments.custody.domain.model.ChainId;

public interface ChainHealthProvider {

    double getHealthScore(ChainId chainId);
}
