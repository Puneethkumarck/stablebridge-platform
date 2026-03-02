plugins {
    `java-library`
}

dependencies {
    api(project(":merchant-onboarding:merchant-onboarding-api"))
    api("org.springframework.cloud:spring-cloud-starter-openfeign")
}
