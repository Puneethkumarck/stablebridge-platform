package com.stablecoin.payments.offramp.domain.port;

public interface PayoutPartnerGateway {

    PayoutResult initiatePayout(PayoutRequest request);
}
