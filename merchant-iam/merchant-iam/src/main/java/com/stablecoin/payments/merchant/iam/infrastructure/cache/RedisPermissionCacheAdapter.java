package com.stablecoin.payments.merchant.iam.infrastructure.cache;

import com.stablecoin.payments.merchant.iam.domain.PermissionCacheProvider;
import com.stablecoin.payments.merchant.iam.domain.team.model.core.Permission;
import com.stablecoin.payments.merchant.iam.domain.team.model.core.PermissionSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisPermissionCacheAdapter implements PermissionCacheProvider {

    static final Duration TTL = Duration.ofSeconds(60);
    private static final String KEY_PREFIX = "perms:";
    // Sentinel stored when the user has no permissions — distinguishes "cached empty" from "cache miss"
    private static final String EMPTY_SENTINEL = "__empty__";

    private final StringRedisTemplate redis;

    @Override
    public Optional<PermissionSet> getPermissions(UUID userId) {
        var value = redis.opsForValue().get(key(userId));
        if (value == null) {
            return Optional.empty();
        }
        if (EMPTY_SENTINEL.equals(value)) {
            return Optional.of(PermissionSet.empty());
        }
        var permissions = Arrays.stream(value.split(","))
                .map(Permission::parse)
                .collect(Collectors.toSet());
        return Optional.of(PermissionSet.of(permissions));
    }

    @Override
    public void putPermissions(UUID userId, PermissionSet permissions) {
        var value = permissions.isEmpty()
                ? EMPTY_SENTINEL
                : permissions.permissions().stream()
                        .map(p -> p.namespace() + ":" + p.action())
                        .collect(Collectors.joining(","));
        redis.opsForValue().set(key(userId), value, TTL);
        log.debug("Cached permissions userId={} count={}", userId, permissions.size());
    }

    @Override
    public void evict(UUID userId) {
        redis.delete(key(userId));
        log.debug("Evicted permission cache userId={}", userId);
    }

    @Override
    public void evictAll(UUID merchantId) {
        // Scan for all keys matching the merchant's users.
        // In production this is replaced by a merchant→users index; here we use SCAN.
        var pattern = KEY_PREFIX + "*";
        var keys = redis.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
            log.debug("Evicted all permission cache entries for merchantId={} keys={}", merchantId, keys.size());
        }
    }

    private String key(UUID userId) {
        return KEY_PREFIX + userId;
    }
}
