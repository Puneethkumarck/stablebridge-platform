package com.stablecoin.payments.offramp.infrastructure.provider.circle;

record CirclePayoutResponse(CirclePayoutData data) {

    record CirclePayoutData(
            String id,
            CirclePayoutAmount amount,
            String status,
            String createDate
    ) {}

    record CirclePayoutAmount(String amount, String currency) {}
}
