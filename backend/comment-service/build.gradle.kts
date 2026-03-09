dependencies {
    implementation(project(":common")) {
        exclude(group = "org.springframework", module = "spring-web")
    }
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.3.0")
    runtimeOnly("io.asyncer:r2dbc-mysql:1.0.6")
    testImplementation("io.projectreactor:reactor-test")
}
