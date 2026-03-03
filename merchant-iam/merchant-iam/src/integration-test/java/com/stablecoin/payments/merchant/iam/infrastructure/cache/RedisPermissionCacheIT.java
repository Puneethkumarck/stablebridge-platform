package com.stablecoin.payments.merchant.iam.infrastructure.cache;

import com.stablecoin.payments.merchant.iam.AbstractIntegrationTest;
import com.stablecoin.payments.merchant.iam.domain.PermissionCacheProvider;
import com.stablecoin.payments.merchant.iam.domain.team.model.core.Permission;
import com.stablecoin.payments.merchant.iam.domain.team.model.core.PermissionSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RedisPermissionCacheIT extends AbstractIntegrationTest {

    @Autowired
    private PermissionCacheProvider cache;

    @Autowired
    private StringRedisTemplate redis;

    @BeforeEach
    void flushRedis() {
        var keys = redis.keys("perms:*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    @Test
    void returns_empty_on_cache_miss() {
        var result = cache.getPermissions(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    void stores_and_retrieves_permissions() {
        var userId = UUID.randomUUID();
        var permissions = PermissionSet.of(List.of(
                Permission.of("payments", "read"),
                Permission.of("transactions", "read")));

        cache.putPermissions(userId, permissions);
        var result = cache.getPermissions(userId);

        assertThat(result).isPresent();
        assertThat(result.get().has(Permission.of("payments", "read"))).isTrue();
        assertThat(result.get().has(Permission.of("transactions", "read"))).isTrue();
        assertThat(result.get().size()).isEqualTo(2);
    }

    @Test
    void stores_and_retrieves_empty_permission_set() {
        var userId = UUID.randomUUID();

        cache.putPermissions(userId, PermissionSet.empty());
        var result = cache.getPermissions(userId);

        assertThat(result).isPresent();
        assertThat(result.get().isEmpty()).isTrue();
    }

    @Test
    void evict_removes_entry() {
        var userId = UUID.randomUUID();
        cache.putPermissions(userId, PermissionSet.of(List.of(Permission.of("payments", "read"))));

        cache.evict(userId);

        assertThat(cache.getPermissions(userId)).isEmpty();
    }

    @Test
    void evict_all_removes_all_entries() {
        var userId1 = UUID.randomUUID();
        var userId2 = UUID.randomUUID();
        var merchantId = UUID.randomUUID();

        cache.putPermissions(userId1, PermissionSet.of(List.of(Permission.of("payments", "read"))));
        cache.putPermissions(userId2, PermissionSet.of(List.of(Permission.of("roles", "read"))));

        cache.evictAll(merchantId);

        assertThat(cache.getPermissions(userId1)).isEmpty();
        assertThat(cache.getPermissions(userId2)).isEmpty();
    }

    @Test
    void wildcard_permission_survives_round_trip() {
        var userId = UUID.randomUUID();
        var permissions = PermissionSet.of(List.of(Permission.of("*", "*")));

        cache.putPermissions(userId, permissions);
        var result = cache.getPermissions(userId);

        assertThat(result).isPresent();
        assertThat(result.get().has(Permission.of("payments", "write"))).isTrue();
        assertThat(result.get().has(Permission.of("compliance", "read"))).isTrue();
    }

    @Test
    void key_is_prefixed_with_perms() {
        var userId = UUID.randomUUID();

        cache.putPermissions(userId, PermissionSet.of(List.of(Permission.of("payments", "read"))));

        assertThat(redis.hasKey("perms:" + userId)).isTrue();
    }

    @Test
    void ttl_is_set_on_cached_entry() {
        var userId = UUID.randomUUID();

        cache.putPermissions(userId, PermissionSet.of(List.of(Permission.of("payments", "read"))));

        var ttl = redis.getExpire("perms:" + userId);
        assertThat(ttl).isPositive();
        assertThat(ttl).isLessThanOrEqualTo(RedisPermissionCacheAdapter.TTL.getSeconds());
    }
}
