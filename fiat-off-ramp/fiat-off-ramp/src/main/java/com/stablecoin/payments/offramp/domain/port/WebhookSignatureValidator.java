package com.stablecoin.payments.offramp.domain.port;

public interface WebhookSignatureValidator {

    boolean isValid(String payload, String signature);
}
