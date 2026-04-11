package com.stablecoin.payments.gateway.iam.infrastructure.auth;

import com.stablecoin.payments.gateway.iam.domain.exception.UserJwksUnavailableException;
import com.stablecoin.payments.gateway.iam.infrastructure.client.MerchantIamClient;
import com.stablecoin.payments.gateway.iam.infrastructure.config.MerchantIamProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class CachedUserJwksProviderTest {

    private static final String CACHE_KEY = "jwks:merchant-iam";
    private static final String JWKS_JSON = "{\"keys\":[{\"kty\":\"EC\"}]}";

    @Mock
    private MerchantIamClient merchantIamClient;

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOps;

    private CachedUserJwksProvider provider;

    @BeforeEach
    void setUp() {
        var properties = new MerchantIamProperties(
                "http://localhost:8083",
                "https://api.stablebridge.dev",
                "payment-platform",
                24);
        provider = new CachedUserJwksProvider(merchantIamClient, redis, properties);
        given(redis.opsForValue()).willReturn(valueOps);
    }

    @Nested
    class WhenS13Available {

        @Test
        void shouldFetchAndCacheJwks() {
            given(valueOps.get(CACHE_KEY)).willReturn(null);
            given(merchantIamClient.fetchJwks()).willReturn(JWKS_JSON);

            var result = provider.fetchJwks();

            assertThat(result).isEqualTo(JWKS_JSON);
            then(valueOps).should().set(CACHE_KEY, JWKS_JSON, Duration.ofHours(24));
        }

        @Test
        void shouldRefreshCacheEvenWhenCachedValueExists() {
            given(valueOps.get(CACHE_KEY)).willReturn("old-jwks");
            given(merchantIamClient.fetchJwks()).willReturn(JWKS_JSON);

            var result = provider.fetchJwks();

            assertThat(result).isEqualTo(JWKS_JSON);
            then(valueOps).should().set(CACHE_KEY, JWKS_JSON, Duration.ofHours(24));
        }
    }

    @Nested
    class WhenS13Unavailable {

        @Test
        void shouldReturnCachedValueWhenAvailable() {
            given(valueOps.get(CACHE_KEY)).willReturn(JWKS_JSON);
            given(merchantIamClient.fetchJwks()).willThrow(new RuntimeException("Connection refused"));

            var result = provider.fetchJwks();

            assertThat(result).isEqualTo(JWKS_JSON);
        }

        @Test
        void shouldThrowWhenNoCachedValue() {
            given(valueOps.get(CACHE_KEY)).willReturn(null);
            given(merchantIamClient.fetchJwks()).willThrow(new RuntimeException("Connection refused"));

            assertThatThrownBy(() -> provider.fetchJwks())
                    .isInstanceOf(UserJwksUnavailableException.class)
                    .hasMessageContaining("no cached value available");
        }
    }
}
