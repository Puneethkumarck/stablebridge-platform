plugins {
    `java-library`
}

val namastackVersion: String by project

dependencies {
    api(project(":platform-api"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.cloud:spring-cloud-starter-vault-config")
    implementation("io.namastack:namastack-outbox-starter-jdbc:$namastackVersion")

    // Micrometer — compileOnly so services pick the actual registry via their own starter;
    // the MetricsConfig activates only when MeterRegistry is on the classpath.
    compileOnly("io.micrometer:micrometer-core")

    // Feign — compileOnly so services without Feign are not forced to pull it in;
    // the interceptor activates only when RequestInterceptor is on the classpath.
    compileOnly("org.springframework.cloud:spring-cloud-starter-openfeign")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-kafka")
    testImplementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    testImplementation("io.micrometer:micrometer-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
