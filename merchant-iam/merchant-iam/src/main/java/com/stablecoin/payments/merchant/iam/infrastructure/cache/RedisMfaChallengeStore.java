package com.stablecoin.payments.merchant.iam.infrastructure.cache;

import com.stablecoin.payments.merchant.iam.domain.team.MfaChallengeStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis-backed MFA challenge store.
 * Key:   {@code mfa:challenge:{challengeId}}
 * Value: {@code userId:merchantId:emailHash}
 * TTL:   5 minutes (challenges expire)
 * The key is consumed (deleted) on first successful read — one-time use.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMfaChallengeStore implements MfaChallengeStore {

    static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);
    private static final String KEY_PREFIX = "mfa:challenge:";

    private final StringRedisTemplate redis;

    @Override
    public String store(UUID userId, UUID merchantId, String emailHash) {
        var challengeId = UUID.randomUUID().toString();
        var value = userId + ":" + merchantId + ":" + emailHash;
        redis.opsForValue().set(KEY_PREFIX + challengeId, value, CHALLENGE_TTL);
        log.debug("MFA challenge stored challengeId={} userId={}", challengeId, userId);
        return challengeId;
    }

    @Override
    public Optional<Challenge> consume(String challengeId) {
        var key = KEY_PREFIX + challengeId;
        var value = redis.opsForValue().getAndDelete(key);
        if (value == null) {
            log.debug("MFA challenge not found or expired challengeId={}", challengeId);
            return Optional.empty();
        }
        var parts = value.split(":", 3);
        if (parts.length != 3) {
            log.warn("Malformed MFA challenge value challengeId={}", challengeId);
            return Optional.empty();
        }
        return Optional.of(new Challenge(
                UUID.fromString(parts[0]),
                UUID.fromString(parts[1]),
                parts[2]));
    }
}
