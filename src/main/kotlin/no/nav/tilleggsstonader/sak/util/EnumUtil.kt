package no.nav.tilleggsstonader.sak.util

fun enumTilVisningsnavn(enum: String): String =
    enum
        .replace('_', ' ')
        .lowercase()
        .replaceFirstChar { it.uppercase() }
