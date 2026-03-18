plugins {
    id("stablebridge.service")
}

val temporalVersion: String by project
val wiremockVersion: String by project

stablebridge {
    jibImageName.set("stablebridge/merchant-onboarding")
}

dependencies {
    implementation(project(":merchant-onboarding:merchant-onboarding-api"))

    // Temporal
    implementation("io.temporal:temporal-spring-boot-starter:$temporalVersion")

    // Test
    testImplementation("io.temporal:temporal-testing:$temporalVersion")
    testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
}
