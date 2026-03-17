plugins {
    id("stablebridge.service")
}

val temporalVersion: String by project
val wiremockVersion: String by project

stablebridge {
    jibImageName.set("stablebridge/payment-orchestrator")
}

dependencies {
    implementation(project(":payment-orchestrator:payment-orchestrator-api"))
    implementation(project(":compliance-travel-rule:compliance-travel-rule-client"))
    implementation(project(":fx-liquidity-engine:fx-liquidity-engine-client"))
    implementation(project(":fiat-on-ramp:fiat-on-ramp-client"))
    implementation(project(":blockchain-custody:blockchain-custody-client"))
    implementation(project(":fiat-off-ramp:fiat-off-ramp-client"))

    // Redis — payment state cache
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Temporal SDK
    implementation("io.temporal:temporal-sdk:$temporalVersion")
    implementation("io.temporal:temporal-spring-boot-starter:$temporalVersion")

    // Test Fixtures — cross-service client deps
    testFixturesImplementation(project(":compliance-travel-rule:compliance-travel-rule-client"))
    testFixturesImplementation(project(":fx-liquidity-engine:fx-liquidity-engine-client"))

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
    testImplementation("io.temporal:temporal-testing:$temporalVersion")
}
