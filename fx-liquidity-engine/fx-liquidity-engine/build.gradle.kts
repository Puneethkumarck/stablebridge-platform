plugins {
    id("stablebridge.service")
}

val resilience4jVersion: String by project

stablebridge {
    jibImageName.set("stablebridge/fx-liquidity-engine")
    jacocoMinimum.set("0.45")
}

dependencies {
    implementation(project(":fx-liquidity-engine:fx-liquidity-engine-api"))

    // Redis — rate cache
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Resilience4j retry (service-specific)
    implementation("io.github.resilience4j:resilience4j-retry:$resilience4jVersion")

    // Test Fixtures — JPA for test helpers
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Test — WireMock for adapter unit tests
    testImplementation("org.wiremock:wiremock-standalone:${project.property("wiremockVersion")}")
}
