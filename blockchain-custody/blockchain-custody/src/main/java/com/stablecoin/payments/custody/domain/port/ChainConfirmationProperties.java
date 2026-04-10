package com.stablecoin.payments.custody.domain.port;

public interface ChainConfirmationProperties {

    int getMinConfirmations(String chainId);
}
