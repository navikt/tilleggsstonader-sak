package no.nav.tilleggsstonader.sak.integrasjonstest.dsl

// For at man kun skal kunne aksessere funksjoner i sitt scope i DSL'en
// https://kotlinlang.org/docs/type-safe-builders.html#scope-control-dslmarker
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class BehandlingTestdataDslMarker
