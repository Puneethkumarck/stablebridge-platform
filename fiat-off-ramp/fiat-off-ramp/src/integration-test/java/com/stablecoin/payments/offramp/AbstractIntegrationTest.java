package com.stablecoin.payments.offramp;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import static com.stablecoin.payments.platform.test.TestContainerSupport.kafka;
import static com.stablecoin.payments.platform.test.TestContainerSupport.postgres;
import static com.stablecoin.payments.platform.test.TestContainerSupport.registerKafkaProperties;
import static com.stablecoin.payments.platform.test.TestContainerSupport.registerPostgresProperties;
import static com.stablecoin.payments.platform.test.TestContainerSupport.startAll;

@SuppressWarnings("resource")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = postgres("s5_fiat_off_ramp");
    protected static final KafkaContainer KAFKA = kafka();

    static {
        startAll(POSTGRES, KAFKA);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    off_ramp_transactions,
                    stablecoin_redemptions,
                    payout_orders,
                    offramp_outbox_record,
                    offramp_outbox_instance,
                    offramp_outbox_partition
                CASCADE
                """);
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, POSTGRES);
        registerKafkaProperties(registry, KAFKA);
    }
}
