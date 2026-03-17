plugins {
    id("stablebridge.service")
}

val resilience4jVersion: String by project

stablebridge {
    jibImageName.set("stablebridge/fiat-on-ramp")
}

dependencies {
    implementation(project(":fiat-on-ramp:fiat-on-ramp-api"))

    // Resilience4j retry (service-specific)
    implementation("io.github.resilience4j:resilience4j-retry:$resilience4jVersion")

    // Test Fixtures
    testFixturesImplementation(project(":fiat-on-ramp:fiat-on-ramp-api"))

    // Test — WireMock for adapter unit tests
    testImplementation("org.wiremock:wiremock-standalone:${project.property("wiremockVersion")}")
}
