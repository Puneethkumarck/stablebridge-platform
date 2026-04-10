package com.stablecoin.payments.onramp.infrastructure.provider.stripe;

import com.fasterxml.jackson.annotation.JsonProperty;

record StripeRefundResponse(
        String id,
        String object,
        String status,
        Long amount,
        String currency,
        @JsonProperty("payment_intent") String paymentIntent,
        String charge,
        String reason,
        Long created
) {
}
