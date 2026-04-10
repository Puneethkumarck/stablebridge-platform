package com.stablecoin.payments.platform.infrastructure.http;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
