package com.stablecoin.payments.onramp.domain.port;

public interface WebhookSignatureValidator {

    boolean isValid(String payload, String signature);
}
