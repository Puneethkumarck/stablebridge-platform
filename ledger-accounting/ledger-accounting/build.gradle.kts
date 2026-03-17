plugins {
    id("stablebridge.service")
}

stablebridge {
    jibImageName.set("stablebridge/ledger-accounting")
    extraJacocoExclusions.set(listOf("**/filter/**", "**/controller/GlobalExceptionHandler*"))
}

dependencies {
    implementation(project(":ledger-accounting:ledger-accounting-api"))

    // Test Fixtures
    testFixturesImplementation(project(":ledger-accounting:ledger-accounting-api"))
}
