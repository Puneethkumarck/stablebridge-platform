package com.stablecoin.payments.custody.domain.port;

import com.stablecoin.payments.custody.domain.model.ChainId;
import com.stablecoin.payments.custody.domain.model.StablecoinTicker;

public interface TokenContractResolver {

    String resolveContract(ChainId chainId, StablecoinTicker stablecoin);
}
