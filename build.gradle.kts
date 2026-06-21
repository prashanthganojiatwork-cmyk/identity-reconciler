plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "identity-reconciler"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testCompileOnly("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testAnnotationProcessor("org.projectlombok:lombok")
    testImplementation("net.jqwik:jqwik:1.8.0")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.2.0")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.apache.commons:commons-text:1.11.0")
    implementation("commons-codec:commons-codec:1.16.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
