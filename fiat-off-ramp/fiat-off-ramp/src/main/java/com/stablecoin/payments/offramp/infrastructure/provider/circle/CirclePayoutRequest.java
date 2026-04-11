package com.stablecoin.payments.offramp.infrastructure.provider.circle;

record CirclePayoutRequest(
        String idempotencyKey,
        CircleDestination destination,
        CircleAmount amount
) {

    record CircleDestination(String type, String id) {}

    record CircleAmount(String amount, String currency) {}
}
