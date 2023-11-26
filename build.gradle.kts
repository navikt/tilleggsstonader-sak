val javaVersion = JavaLanguageVersion.of(17)
val familieProsesseringVersion = "2.20230926054831_994885a"
val tilleggsstønaderLibsVersion = "2023.10.17-11.20.65cb9a56f6df"
val tilleggsstønaderKontrakterVersion = "2023.11.08-08.00.f2ea26beb3e8"
val tokenSupportVersion = "3.1.5"
val wiremockVersion = "3.2.0"
val mockkVersion = "1.13.8"
val testcontainerVersion = "1.19.0"

group = "no.nav.tilleggsstonader.sak"
version = "1.0.0"

plugins {
    application

    kotlin("jvm") version "1.9.10"
    id("com.diffplug.spotless") version "6.21.0"
    id("com.github.ben-manes.versions") version "0.48.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.18"

    id("org.springframework.boot") version "3.1.4"
    id("io.spring.dependency-management") version "1.1.3"
    kotlin("plugin.spring") version "1.9.10"

    id("org.cyclonedx.bom") version "1.7.4"
}

repositories {
    mavenCentral()
    mavenLocal()

    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

apply(plugin = "com.diffplug.spotless")

spotless {
    kotlin {
        ktlint("0.50.0")
    }
}

configurations.all {
    resolutionStrategy {
        failOnNonReproducibleResolution()
    }
}

dependencies {
    // Spring
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")

    // Logging
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    implementation("io.micrometer:micrometer-registry-prometheus")

    implementation("no.nav.familie:prosessering-core:$familieProsesseringVersion")

    // Tillegggsstønader libs
    implementation("no.nav.tilleggsstonader-libs:util:$tilleggsstønaderLibsVersion")
    implementation("no.nav.tilleggsstonader-libs:log:$tilleggsstønaderLibsVersion")
    implementation("no.nav.tilleggsstonader-libs:http-client:$tilleggsstønaderLibsVersion")
    implementation("no.nav.tilleggsstonader-libs:sikkerhet:$tilleggsstønaderLibsVersion")

    implementation("no.nav.tilleggsstonader.kontrakter:tilleggsstonader-kontrakter:$tilleggsstønaderKontrakterVersion")

    // For auditlogger. August, 2014, men det er den som blir brukt på NAV
    implementation("com.papertrailapp:logback-syslog4j:1.0.0")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "mockito-core")
    }
    testImplementation("org.junit.platform:junit-platform-suite")
    testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")

    testImplementation("org.testcontainers:postgresql:$testcontainerVersion")

    testImplementation("no.nav.security:token-validation-spring-test:$tokenSupportVersion")
    testImplementation("no.nav.tilleggsstonader-libs:test-util:$tilleggsstønaderLibsVersion")

    testImplementation(platform("io.cucumber:cucumber-bom:7.14.1"))
    testImplementation("io.cucumber:cucumber-java")
    testImplementation("io.cucumber:cucumber-junit-platform-engine")
}

kotlin {
    jvmToolchain(javaVersion.asInt())

    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

application {
    mainClass.set("no.nav.tilleggsstonader.sak.AppKt")
}

if (project.hasProperty("skipLint")) {
    gradle.startParameter.excludedTaskNames += "spotlessKotlinCheck"
}

tasks.test {
    useJUnitPlatform()
    // Work around. Gradle does not include enough information to disambiguate
    // between different examples and scenarios.
    systemProperty("cucumber.junit-platform.naming-strategy", "long")
}

tasks.bootJar {
    archiveFileName.set("app.jar")
}
