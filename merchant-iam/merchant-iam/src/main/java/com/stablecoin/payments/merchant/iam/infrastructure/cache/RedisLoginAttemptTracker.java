package com.stablecoin.payments.merchant.iam.infrastructure.cache;

import com.stablecoin.payments.merchant.iam.domain.team.LoginAttemptTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

/**
 * Redis-backed brute-force protection.
 * Key: {@code login:fail:{emailHash}}
 * Value: failure count as string
 * TTL: 15 minutes, reset on each failure (sliding window)
 * Threshold: 5 failures triggers lockout
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLoginAttemptTracker implements LoginAttemptTracker {

    static final int MAX_FAILURES = 5;
    static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);
    private static final String KEY_PREFIX = "login:fail:";

    private final StringRedisTemplate redis;

    @Override
    public int recordFailure(String emailHash) {
        var key = key(emailHash);
        var count = redis.opsForValue().increment(key);
        redis.expire(key, LOCKOUT_DURATION);
        log.debug("Login failure recorded emailHash={} count={}", emailHash, count);
        return Objects.requireNonNull(count).intValue();
    }

    @Override
    public boolean isLockedOut(String emailHash) {
        var value = redis.opsForValue().get(key(emailHash));
        if (value == null) {
            return false;
        }
        return Integer.parseInt(value) >= MAX_FAILURES;
    }

    @Override
    public void clearFailures(String emailHash) {
        redis.delete(key(emailHash));
        log.debug("Login failures cleared emailHash={}", emailHash);
    }

    private String key(String emailHash) {
        return KEY_PREFIX + emailHash;
    }
}
