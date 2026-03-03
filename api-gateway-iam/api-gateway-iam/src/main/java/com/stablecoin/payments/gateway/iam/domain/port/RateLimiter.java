package com.stablecoin.payments.gateway.iam.domain.port;

import java.util.UUID;

import com.stablecoin.payments.gateway.iam.domain.model.RateLimitPolicy;

public interface RateLimiter {

    RateLimitResult check(UUID merchantId, String endpoint, RateLimitPolicy policy);

    record RateLimitResult(boolean allowed, int currentCount, int limit, String window) {}
}
