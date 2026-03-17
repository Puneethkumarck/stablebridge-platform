package com.stablecoin.payments.orchestrator;

import jakarta.persistence.EntityManager;
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

    static final PostgreSQLContainer<?> POSTGRES = postgres("s1_payment_orchestrator");
    protected static final KafkaContainer KAFKA = kafka();
    protected static final GenericContainer<?> REDIS = redis();

    static {
        startAll(POSTGRES, KAFKA, REDIS);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    payment_audit_log,
                    payments,
                    orchestrator_outbox_record
                CASCADE
                """);
        entityManager.clear();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, POSTGRES);
        registerKafkaProperties(registry, KAFKA);
        registerRedisProperties(registry, REDIS);
        registry.add("app.security.enabled", () -> "false");
    }
}
