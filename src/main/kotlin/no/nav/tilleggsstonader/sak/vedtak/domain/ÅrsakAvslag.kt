package no.nav.tilleggsstonader.sak.vedtak.domain

enum class ÅrsakAvslag(
    val displayName: String,
) {
    INGEN_AKTIVITET("Ingen aktivitet"),
    IKKE_I_MÅLGRUPPE("Ikke i målgruppe"),
    INGEN_OVERLAPP_AKTIVITET_MÅLGRUPPE("Ingen overlapp aktivitet målgruppe"),
    MANGELFULL_DOKUMENTASJON("Mangelfull dokumentasjon"),
    HAR_IKKE_UTGIFTER("Har ikke utgifter"),
    RETT_TIL_UTSTYRSSTIPEND("Rett til utstyrsstipend"),
    HAR_IKKE_MERUTGIFTER("Har ikke merutgifter"),
    RETT_TIL_BOSTØTTE("Rett til bostøtte"),
    ANNET("Annet"),
}

fun List<ÅrsakAvslag>.formaterListe(): String = joinToString(separator = ", ") { it.displayName }
