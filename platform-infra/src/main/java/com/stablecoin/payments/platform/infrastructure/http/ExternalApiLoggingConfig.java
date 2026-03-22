package com.stablecoin.payments.platform.infrastructure.http;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link ExternalApiLoggingInterceptor} when
 * {@code app.external-api.logging.enabled=true}.
 * <p>
 * Disabled by default — enable in sandbox/dev profiles only.
 * Body truncation controlled by {@code app.external-api.logging.max-body-length} (default 2000).
 */
@Configuration
@ConditionalOnProperty(name = "app.external-api.logging.enabled", havingValue = "true")
public class ExternalApiLoggingConfig {

    @Bean
    ExternalApiLoggingInterceptor externalApiLoggingInterceptor(
            org.springframework.core.env.Environment env) {
        var maxBodyLength = env.getProperty(
                "app.external-api.logging.max-body-length", Integer.class, 2000);
        return new ExternalApiLoggingInterceptor(maxBodyLength);
    }
}
