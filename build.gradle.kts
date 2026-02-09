import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

val javaVersion = JavaLanguageVersion.of(21)
val familieProsesseringVersion = "2.20260120121808_b5446a9"
val tilleggsstønaderLibsVersion = "2026.02.08-22.17.8810439febce"
val tilleggsstønaderKontrakterVersion = "2026.02.06-15.30.d4588360bd7e"
val avroVersion = "1.12.1"
val confluentVersion = "8.0.1"
val joarkHendelseVersion = "1.1.8"
val tokenSupportVersion = "6.0.2"
val wiremockVersion = "3.13.2"
val mockkVersion = "1.14.9"
val testcontainerVersion = "1.21.4"
val springDocVersion = "3.0.1"

group = "no.nav.tilleggsstonader.sak"
version = "1.0.0"

plugins {
    application

    kotlin("jvm") version "2.2.21"
    id("com.diffplug.spotless") version "8.2.1"
    id("com.github.ben-manes.versions") version "0.53.0"
    id("se.patrikerdes.use-latest-versions") version "0.2.19"

    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.spring") version "2.2.21"
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
        ktlint("1.7.1")
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
    implementation("org.springframework.boot:spring-boot-starter-jackson")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springframework.boot:spring-boot-starter-flyway")

    // Kafka
    implementation("org.springframework.boot:spring-boot-starter-kafka")
    implementation("org.apache.avro:avro:$avroVersion")
    implementation("io.confluent:kafka-avro-serializer:$confluentVersion")
    implementation("no.nav.teamdokumenthandtering:teamdokumenthandtering-avro-schemas:$joarkHendelseVersion")

    // Logging
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")

    implementation("io.micrometer:micrometer-registry-prometheus")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")

    implementation("no.nav.familie:prosessering-core:$familieProsesseringVersion")

    // Tillegggsstønader libs
    implementation("no.nav.tilleggsstonader-libs:util:$tilleggsstønaderLibsVersion")
    implementation("no.nav.tilleggsstonader-libs:log:$tilleggsstønaderLibsVersion")
    implementation("no.nav.tilleggsstonader-libs:http-client:$tilleggsstønaderLibsVersion")
    implementation("no.nav.tilleggsstonader-libs:sikkerhet:$tilleggsstønaderLibsVersion")
    implementation("no.nav.tilleggsstonader-libs:unleash:$tilleggsstønaderLibsVersion")
    implementation("no.nav.tilleggsstonader-libs:kafka:$tilleggsstønaderLibsVersion")
    implementation("no.nav.tilleggsstonader-libs:spring:$tilleggsstønaderLibsVersion")

    implementation("no.nav.tilleggsstonader.kontrakter:kontrakter-felles:$tilleggsstønaderKontrakterVersion")
    implementation("no.nav.tilleggsstonader.kontrakter:pdl-personhendelser-avro:$tilleggsstønaderKontrakterVersion")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "mockito-core")
    }
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-resttestclient")

    testImplementation("org.junit.platform:junit-platform-suite")
    testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")

    testImplementation("org.testcontainers:postgresql:$testcontainerVersion")

    testImplementation("no.nav.security:token-validation-spring-test:$tokenSupportVersion")
    testImplementation("no.nav.tilleggsstonader-libs:test-util:$tilleggsstønaderLibsVersion")

    testImplementation(platform("io.cucumber:cucumber-bom:7.34.2"))
    testImplementation("io.cucumber:cucumber-java")
    testImplementation("io.cucumber:cucumber-junit-platform-engine")

    // Transitiv avhengighet fra mock-oauth2-server -> bcpix. Disse under er definert som dynamisk versjon, noe bygget vårt ikke vil ha noe av
    testImplementation("org.bouncycastle:bcutil-jdk18on:1.83")
    testImplementation("org.bouncycastle:bcprov-jdk18on:1.83")
}

kotlin {
    jvmToolchain(javaVersion.asInt())

    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
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
        val execResult =
            providers.exec {
                commandLine = "git rev-parse --verify HEAD".split("\\s".toRegex())
                workingDir = project.projectDir
            }
        return execResult.standardOutput.asText
            .get()
            .trim()
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
