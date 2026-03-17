plugins {
    `java-library`
    `java-test-fixtures`
}

val archunitVersion: String by project
val testcontainersVersion: String by project

dependencies {
    testFixturesApi("org.assertj:assertj-core")
    testFixturesApi("org.mockito:mockito-core")
    testFixturesApi("com.tngtech.archunit:archunit-junit5:$archunitVersion")
    testFixturesApi("org.testcontainers:postgresql:$testcontainersVersion")
    testFixturesApi("org.testcontainers:kafka:$testcontainersVersion")
    testFixturesApi("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testFixturesApi("org.springframework.boot:spring-boot-starter-test")
    testFixturesApi("org.springframework.boot:spring-boot-starter-webmvc-test")
    testFixturesApi("org.springframework.boot:spring-boot-starter-security-test")
    testFixturesApi("org.springframework.boot:spring-boot-starter-security")
}
