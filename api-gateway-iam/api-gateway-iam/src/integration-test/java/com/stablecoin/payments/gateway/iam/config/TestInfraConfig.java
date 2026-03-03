package com.stablecoin.payments.gateway.iam.config;

import com.stablecoin.payments.gateway.iam.domain.model.RateLimitPolicy;
import com.stablecoin.payments.gateway.iam.domain.port.ApiKeyGenerator;
import com.stablecoin.payments.gateway.iam.domain.port.ApiKeyHasher;
import com.stablecoin.payments.gateway.iam.domain.port.ClientSecretHasher;
import com.stablecoin.payments.gateway.iam.domain.port.EventPublisher;
import com.stablecoin.payments.gateway.iam.domain.port.RateLimiter;
import com.stablecoin.payments.gateway.iam.domain.port.TokenIssuer;
import com.stablecoin.payments.gateway.iam.domain.port.TokenRevocationCache;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Provides stub implementations of infrastructure ports that are not yet
 * implemented (coming in PR5: Auth + Cache + Messaging).
 */
@TestConfiguration
public class TestInfraConfig {

    @Bean
    public TokenIssuer tokenIssuer() {
        return new TokenIssuer() {
            @Override
            public String issueToken(UUID merchantId, UUID clientId, List<String> scopes) {
                return "test-jwt-token";
            }

            @Override
            public String jwksJson() {
                return "{\"keys\":[]}";
            }
        };
    }

    @Bean
    public TokenRevocationCache tokenRevocationCache() {
        return new TokenRevocationCache() {
            @Override
            public void markRevoked(UUID jti, Duration ttl) {
                // no-op
            }

            @Override
            public boolean isRevoked(UUID jti) {
                return false;
            }
        };
    }

    @Bean
    public ClientSecretHasher clientSecretHasher() {
        return new ClientSecretHasher() {
            @Override
            public String hash(String rawSecret) {
                return "hashed_" + rawSecret;
            }

            @Override
            public boolean matches(String rawSecret, String hash) {
                return hash.equals("hashed_" + rawSecret);
            }
        };
    }

    @Bean
    public ApiKeyGenerator apiKeyGenerator() {
        return environment -> new ApiKeyGenerator.GeneratedApiKey(
                "pk_" + environment.name().toLowerCase() + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 32),
                "pk_" + environment.name().toLowerCase() + "_abc"
        );
    }

    @Bean
    public ApiKeyHasher apiKeyHasher() {
        return rawKey -> "sha256_" + rawKey;
    }

    @Bean
    public EventPublisher<?> eventPublisher() {
        return event -> {
            // no-op
        };
    }

    @Bean
    public RateLimiter rateLimiter() {
        return (UUID merchantId, String endpoint, RateLimitPolicy policy) ->
                new RateLimiter.RateLimitResult(true, 0, policy.tier().requestsPerMinute(), "1m");
    }
}
