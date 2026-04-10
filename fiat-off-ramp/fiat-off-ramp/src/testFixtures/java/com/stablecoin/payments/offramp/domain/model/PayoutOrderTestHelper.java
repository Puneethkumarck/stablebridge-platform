package com.stablecoin.payments.offramp.domain.model;

import java.time.Instant;

public final class PayoutOrderTestHelper {

    private PayoutOrderTestHelper() {}

    public static PayoutOrder withUpdatedAt(PayoutOrder order, Instant updatedAt) {
        return order.toBuilder().updatedAt(updatedAt).build();
    }
}
