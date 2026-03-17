package com.stablecoin.payments.merchant.onboarding;

import com.stablecoin.payments.merchant.onboarding.config.TestTemporalConfig;
import com.stablecoin.payments.platform.test.TestSecurityConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
@AutoConfigureMockMvc
@Import({TestSecurityConfig.class, TestTemporalConfig.class})
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = postgres("s11_merchant_onboarding");
    protected static final KafkaContainer KAFKA = kafka();

    static {
        startAll(POSTGRES, KAFKA);
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registerPostgresProperties(registry, POSTGRES);
        registerKafkaProperties(registry, KAFKA);
    }
}
