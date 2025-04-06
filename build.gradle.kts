import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.io.ByteArrayOutputStream

val javaVersion = JavaLanguageVersion.of(21)
val familieProsesseringVersion = "2.20250331103528_ed988de"
val tilleggsstønaderLibsVersion = "2025.03.28-12.05.78567a42f6ae"
val tilleggsstønaderKontrakterVersion = "2025.04.04-11.57.4d5d45684538"
val avroVersion = "1.12.0"
val confluentVersion = "7.9.0"
val joarkHendelseVersion = "08271806"
val tokenSupportVersion = "5.0.24"
val wiremockVersion = "3.12.1"
val mockkVersion = "1.13.17"
val testcontainerVersion = "1.20.6"

group = "no.nav.tilleggsstonader.sak"
version = "1.0.0"

plugins {
    application

    kotlin("jvm") version "2.1.20"
    id("com.diffplug.spotless") version "7.0.2"
    id("com.github.ben-manes.versions") version "0.52.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.18"

    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.spring") version "2.1.20"

    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"

    id("org.cyclonedx.bom") version "2.2.0"
}

repositories {
    mavenCentral()
    mavenLocal()

    maven(url = "https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    maven(url = "https://packages.confluent.io/maven/")
}

apply(plugin = "com.diffplug.spotless")

spotless {
    kotlin {
        ktlint("1.5.0")
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
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.apache.avro:avro:$avroVersion")
    implementation("io.confluent:kafka-avro-serializer:$confluentVersion")
    implementation("no.nav.teamdokumenthandtering:teamdokumenthandtering-avro-schemas:$joarkHendelseVersion")

    // Logging
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")

    implementation("io.micrometer:micrometer-registry-prometheus")

    implementation("no.nav.familie:prosessering-core:$familieProsesseringVersion")

    // Tillegggsstønader libs
    implementation("no.nav.tilleggsstonader-libs:util:$tilleggsstønaderLibsVersion")
    implementation("no.nav.tilleggsstonader-libs:log:$tilleggsstønaderLibsVersion")
    implementation("no.nav.tilleggsstonader-libs:http-client:$tilleggsstønaderLibsVersion")
    implementation("no.nav.tilleggsstonader-libs:sikkerhet:$tilleggsstønaderLibsVersion")
    implementation("no.nav.tilleggsstonader-libs:unleash:$tilleggsstønaderLibsVersion")
    implementation("no.nav.tilleggsstonader-libs:kafka:$tilleggsstønaderLibsVersion")

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

    testImplementation(platform("io.cucumber:cucumber-bom:7.22.0"))
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

// Oppretter version.properties med git-sha som version
tasks {
    fun getCheckedOutGitCommitHash(): String {
        if (System.getenv("GITHUB_ACTIONS") == "true") {
            return System.getenv("GITHUB_SHA")
        }
        val byteOut = ByteArrayOutputStream()
        project.exec {
            commandLine = "git rev-parse --verify HEAD".split("\\s".toRegex())
            standardOutput = byteOut
        }
        return String(byteOut.toByteArray()).trim()
    }

    val projectProps by registering(WriteProperties::class) {
        destinationFile = layout.buildDirectory.file("version.properties")
        // Define property.
        property("project.version", getCheckedOutGitCommitHash())
    }

    processResources {
        // Depend on output of the task to create properties,
        // so the properties file will be part of the Java resources.
        from(projectProps)
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events = setOf(TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
        showStackTraces = false
        showCauses = false
    }
    // Work around. Gradle does not include enough information to disambiguate
    // between different examples and scenarios.
    systemProperty("cucumber.junit-platform.naming-strategy", "long")
}

tasks.bootJar {
    archiveFileName.set("app.jar")
}

tasks.cyclonedxBom {
    setIncludeConfigs(listOf("runtimeClasspath", "compileClasspath"))
}
