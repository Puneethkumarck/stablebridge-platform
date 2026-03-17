plugins {
    id("stablebridge.service")
}

val resilience4jVersion: String by project

stablebridge {
    jibImageName.set("stablebridge/fiat-off-ramp")
    extraJacocoExclusions.set(listOf("**/filter/**", "**/controller/GlobalExceptionHandler*"))
}

dependencies {
    implementation(project(":fiat-off-ramp:fiat-off-ramp-api"))

    // Resilience4j retry (service-specific)
    implementation("io.github.resilience4j:resilience4j-retry:$resilience4jVersion")

    // Test Fixtures
    testFixturesImplementation(project(":fiat-off-ramp:fiat-off-ramp-api"))

    // Test — WireMock for adapter unit tests
    testImplementation("org.wiremock:wiremock-standalone:${project.property("wiremockVersion")}")
}
