import java.time.Duration

plugins {
    java
}

val temporalVersion: String by project

dependencies {
    // Jackson 3 for JSON parsing
    testImplementation("tools.jackson.core:jackson-databind")

    // Temporal SDK — workflow client for signal verification
    testImplementation("io.temporal:temporal-sdk:$temporalVersion")

    // Kafka — event verification
    testImplementation("org.apache.kafka:kafka-clients")

    // Test framework
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.awaitility:awaitility")

    testImplementation("org.slf4j:slf4j-api")
    testRuntimeOnly("ch.qos.logback:logback-classic")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    timeout.set(Duration.ofMinutes(10))
    // These tests require the Docker Compose stack to be running
    // Run via: ./scripts/run-phase3-tests.sh
    enabled = System.getenv("PHASE3_TESTS_ENABLED") == "true"
}
