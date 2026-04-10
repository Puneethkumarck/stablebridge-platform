import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    id("org.springframework.boot")
    id("com.google.cloud.tools.jib")
    java
    `java-test-fixtures`
    jacoco
}

// ---------------------------------------------------------------------------
// Extension for per-service customization
// ---------------------------------------------------------------------------
interface StablebridgeServiceExtension {
    val jibImageName: Property<String>
    val jacocoMinimum: Property<String>
    val extraJacocoExclusions: ListProperty<String>
}

val stablebridge = extensions.create<StablebridgeServiceExtension>("stablebridge")
stablebridge.jacocoMinimum.convention("0.50")
stablebridge.extraJacocoExclusions.convention(emptyList())

// ---------------------------------------------------------------------------
// JIB Docker image configuration
// ---------------------------------------------------------------------------
afterEvaluate {
    extensions.configure<com.google.cloud.tools.jib.gradle.JibExtension> {
        from {
            image = "docker://eclipse-temurin:25-jre"
        }
        to {
            image = stablebridge.jibImageName.get()
            tags = setOf("latest")
        }
        container {
            creationTime.set("USE_CURRENT_TIMESTAMP")
        }
    }
}

// ---------------------------------------------------------------------------
// Integration test source set
// ---------------------------------------------------------------------------
val integrationTestSourceSet: SourceSet = sourceSets.create("integrationTest") {
    java.srcDir("src/integration-test/java")
    resources.srcDir("src/integration-test/resources")
    compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
}

configurations {
    named("integrationTestImplementation") { extendsFrom(configurations.testImplementation.get()) }
    named("integrationTestRuntimeOnly") { extendsFrom(configurations.testRuntimeOnly.get()) }
}

tasks.register<Test>("integrationTest") {
    testClassesDirs = integrationTestSourceSet.output.classesDirs
    classpath = integrationTestSourceSet.runtimeClasspath
    shouldRunAfter(tasks.test)
    configure<JacocoTaskExtension> { isEnabled = false }
    exclude("**/Abstract*", "**/config/**")
}

// ---------------------------------------------------------------------------
// Business test source set
// ---------------------------------------------------------------------------
val businessTestSourceSet: SourceSet = sourceSets.create("businessTest") {
    java.srcDir("src/business-test/java")
    resources.srcDir("src/business-test/resources")
    compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output + integrationTestSourceSet.output
    runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output + integrationTestSourceSet.output
}

configurations {
    named("businessTestImplementation") { extendsFrom(configurations.named("integrationTestImplementation").get()) }
    named("businessTestRuntimeOnly") { extendsFrom(configurations.named("integrationTestRuntimeOnly").get()) }
}

tasks.register<Test>("businessTest") {
    testClassesDirs = businessTestSourceSet.output.classesDirs
    classpath = businessTestSourceSet.runtimeClasspath
    shouldRunAfter(tasks.named("integrationTest"))
    failOnNoDiscoveredTests = false
    configure<JacocoTaskExtension> { isEnabled = false }
}

// ---------------------------------------------------------------------------
// Version properties from gradle.properties
// ---------------------------------------------------------------------------
val lombokVersion: String by project
val mapstructVersion: String by project
val lombokMapstructBindingVersion: String by project
val resilience4jVersion: String by project
val flywayVersion: String by project
val archunitVersion: String by project
val testcontainersVersion: String by project
val wiremockVersion: String by project
val springdocVersion: String by project
val namastackVersion: String by project
val logstashLogbackVersion: String by project

// ---------------------------------------------------------------------------
// Common dependencies — every service gets these
// ---------------------------------------------------------------------------
dependencies {
    // Shared runtime infrastructure (AbstractOutboxHandler, AbstractOutboxEventPublisher, BaseGlobalExceptionHandler)
    implementation(project(":platform-infra"))

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // OpenAPI / Swagger UI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")

    // Observability
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:$logstashLogbackVersion")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")

    // Security
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Kafka via Spring Cloud Stream
    implementation("org.springframework.cloud:spring-cloud-stream")
    implementation("org.springframework.cloud:spring-cloud-stream-binder-kafka")
    implementation("org.springframework.kafka:spring-kafka")

    // Feign
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")

    // Resilience4j (circuit breaker — retry is per-service opt-in)
    implementation("io.github.resilience4j:resilience4j-spring-boot3:$resilience4jVersion")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:$resilience4jVersion")
    implementation("io.github.resilience4j:resilience4j-micrometer:$resilience4jVersion")

    // MapStruct
    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:$lombokMapstructBindingVersion")

    // Outbox (namastack)
    implementation("io.namastack:namastack-outbox-starter-jdbc:$namastackVersion")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")

    // Shared test infrastructure (TestUtils, HexagonalArchitectureRules, TestContainerSupport, TestSecurityConfig)
    testFixturesImplementation(testFixtures(project(":platform-test")))
    testFixturesImplementation("org.assertj:assertj-core")
    testFixturesImplementation("org.mockito:mockito-core")

    // Test
    testImplementation(testFixtures(project(":platform-test")))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:$archunitVersion")

    // Integration Test
    "integrationTestImplementation"(testFixtures(project))
    "integrationTestImplementation"(testFixtures(project(":platform-test")))
    "integrationTestImplementation"("org.testcontainers:postgresql:$testcontainersVersion")
    "integrationTestImplementation"("org.testcontainers:kafka:$testcontainersVersion")
    "integrationTestImplementation"("org.testcontainers:junit-jupiter:$testcontainersVersion")
    "integrationTestImplementation"("org.wiremock:wiremock-standalone:$wiremockVersion")
    "integrationTestImplementation"("org.springframework.boot:spring-boot-starter-webmvc-test")
    "integrationTestImplementation"("org.springframework.boot:spring-boot-starter-security-test")
}

// ---------------------------------------------------------------------------
// MapStruct compiler args
// ---------------------------------------------------------------------------
tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf(
        "-Amapstruct.defaultComponentModel=spring",
        "-Amapstruct.unmappedTargetPolicy=IGNORE"
    ))
}

// ---------------------------------------------------------------------------
// Test configuration
// ---------------------------------------------------------------------------
tasks.withType<Test> {
    jvmArgs("-Dnet.bytebuddy.experimental=true")
    useJUnitPlatform {
        excludeTags("sandbox")
    }
    testLogging {
        events("passed", "skipped", "failed")
        showExceptions = true
        showCauses = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
    }
}

// ---------------------------------------------------------------------------
// JaCoCo — disabled until JaCoCo supports Java 25 (0.8.14 is incompatible)
// ---------------------------------------------------------------------------
jacoco {
    toolVersion = "0.8.14"
}

tasks.test {
    configure<JacocoTaskExtension> {
        isEnabled = false
    }
}

tasks.jacocoTestReport {
    enabled = false
}

tasks.jacocoTestCoverageVerification {
    enabled = false
}

val baseJacocoExclusions = listOf(
    "**/entity/**",
    "**/mapper/**",
    "**/config/**",
    "**/*Application*",
    "**/generated/**",
    "**/*MapperImpl*"
)

afterEvaluate {
    val allExclusions = baseJacocoExclusions + stablebridge.extraJacocoExclusions.get()

    tasks.jacocoTestReport {
        dependsOn(tasks.test)
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
        classDirectories.setFrom(files(classDirectories.files.map {
            fileTree(it) { exclude(allExclusions) }
        }))
    }

    tasks.jacocoTestCoverageVerification {
        dependsOn(tasks.jacocoTestReport)
        violationRules {
            rule {
                limit {
                    minimum = stablebridge.jacocoMinimum.get().toBigDecimal()
                }
            }
        }
        classDirectories.setFrom(files(classDirectories.files.map {
            fileTree(it) { exclude(allExclusions) }
        }))
    }
}

// ---------------------------------------------------------------------------
// Wire check task
// ---------------------------------------------------------------------------
tasks.named("check") {
    dependsOn(
        tasks.named("integrationTest"),
        tasks.named("businessTest"),
        tasks.jacocoTestCoverageVerification
    )
}
