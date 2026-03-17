package com.stablecoin.payments.gateway.iam;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import static com.stablecoin.payments.platform.test.TestContainerSupport.kafka;
import static com.stablecoin.payments.platform.test.TestContainerSupport.postgres;
import static com.stablecoin.payments.platform.test.TestContainerSupport.redis;
import static com.stablecoin.payments.platform.test.TestContainerSupport.registerKafkaProperties;
import static com.stablecoin.payments.platform.test.TestContainerSupport.registerPostgresProperties;
import static com.stablecoin.payments.platform.test.TestContainerSupport.registerRedisProperties;
import static com.stablecoin.payments.platform.test.TestContainerSupport.startAll;

@SuppressWarnings("resource")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = postgres("s10_api_gateway_iam");
    protected static final KafkaContainer KAFKA = kafka();
    protected static final GenericContainer<?> REDIS = redis();

    static {
        startAll(POSTGRES, KAFKA, REDIS);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    access_tokens,
                    api_keys,
                    oauth_clients,
                    rate_limit_events,
                    gateway_audit_log,
                    gatewayiam_idempotency_keys,
                    merchants
                CASCADE
                """);
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, POSTGRES);
        registerKafkaProperties(registry, KAFKA);
        registerRedisProperties(registry, REDIS);
    }
}
