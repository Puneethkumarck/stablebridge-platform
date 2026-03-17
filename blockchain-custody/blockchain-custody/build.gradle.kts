plugins {
    id("stablebridge.service")
}

val resilience4jVersion: String by project
val web3jVersion: String by project

stablebridge {
    jibImageName.set("stablebridge/blockchain-custody")
}

dependencies {
    implementation(project(":blockchain-custody:blockchain-custody-api"))

    // Redis — nonce locks, balance cache
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Resilience4j retry (service-specific)
    implementation("io.github.resilience4j:resilience4j-retry:$resilience4jVersion")

    // Dev custody adapter — EVM transaction signing
    implementation("org.web3j:core:$web3jVersion") {
        exclude(group = "org.slf4j") // avoid SLF4J conflicts with Spring Boot
    }

    // Test — WireMock for adapter unit tests
    testImplementation("org.wiremock:wiremock-standalone:${project.property("wiremockVersion")}")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
}
