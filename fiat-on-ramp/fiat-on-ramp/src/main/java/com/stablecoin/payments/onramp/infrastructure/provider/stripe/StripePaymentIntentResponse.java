package com.stablecoin.payments.onramp.infrastructure.provider.stripe;

import com.fasterxml.jackson.annotation.JsonProperty;

record StripePaymentIntentResponse(
        String id,
        String object,
        String status,
        Long amount,
        @JsonProperty("amount_received") Long amountReceived,
        String currency,
        @JsonProperty("client_secret") String clientSecret,
        @JsonProperty("capture_method") String captureMethod,
        @JsonProperty("payment_method") String paymentMethod,
        Long created,
        String description
) {
}
