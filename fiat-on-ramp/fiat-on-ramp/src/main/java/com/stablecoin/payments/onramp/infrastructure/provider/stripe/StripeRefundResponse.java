package com.stablecoin.payments.onramp.infrastructure.provider.stripe;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * ACL DTO mapping the Stripe Refund response.
 * <p>
 * Package-private — never leaks outside the Stripe adapter package.
 * Uses wrapper types (not primitives) to handle nullable JSON fields safely.
 *
 * @see <a href="https://docs.stripe.com/api/refunds/object">Stripe Refund object</a>
 */
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
