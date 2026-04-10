package com.stablecoin.payments.offramp.domain.port;

public interface RedemptionGateway {

    RedemptionResult redeem(RedemptionRequest request);
}
