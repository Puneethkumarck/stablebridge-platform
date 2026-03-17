plugins {
    id("stablebridge.service")
}

stablebridge {
    jibImageName.set("stablebridge/api-gateway-iam")
    jacocoMinimum.set("0.45")
}

dependencies {
    implementation(project(":api-gateway-iam:api-gateway-iam-api"))

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Auth — JWT ES256 (Nimbus, BOM-managed via oauth2-jose)
    implementation("org.springframework.security:spring-security-oauth2-jose")
}
