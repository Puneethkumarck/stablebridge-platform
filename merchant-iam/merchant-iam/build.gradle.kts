plugins {
    id("stablebridge.service")
}

stablebridge {
    jibImageName.set("stablebridge/merchant-iam")
}

dependencies {
    implementation(project(":merchant-iam:merchant-iam-api"))

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Email
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // Auth — JWT ES256 (Nimbus, BOM-managed via oauth2-jose)
    implementation("org.springframework.security:spring-security-oauth2-jose")

    // Auth — TOTP RFC 6238 (Google Authenticator compatible)
    implementation("dev.samstevens.totp:totp:1.7.1")
}
