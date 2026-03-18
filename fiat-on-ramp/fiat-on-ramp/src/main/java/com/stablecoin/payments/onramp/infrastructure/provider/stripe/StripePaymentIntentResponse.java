package com.stablecoin.payments.onramp.infrastructure.provider.stripe;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * ACL DTO mapping the Stripe PaymentIntent response.
 * <p>
 * Package-private — never leaks outside the Stripe adapter package.
 * Uses wrapper types (not primitives) to handle nullable JSON fields safely.
 *
 * @see <a href="https://docs.stripe.com/api/payment_intents/object">Stripe PaymentIntent object</a>
 */
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
