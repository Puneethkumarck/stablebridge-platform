plugins {
    id("stablebridge.service")
}

val resilience4jVersion: String by project

stablebridge {
    jibImageName.set("stablebridge/compliance-travel-rule")
}

dependencies {
    implementation(project(":compliance-travel-rule:compliance-travel-rule-api"))

    // Redis — KYC cache
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Resilience4j retry (service-specific)
    implementation("io.github.resilience4j:resilience4j-retry:$resilience4jVersion")

    // Test — WireMock for adapter unit tests
    testImplementation("org.wiremock:wiremock-standalone:${project.property("wiremockVersion")}")
}
