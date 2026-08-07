rootProject.name = "tilleggsstonader-sak"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Avkommenter hvis du vil bruke en lokal versjon av kontrakter
includeBuild("../tilleggsstonader-kontrakter")
