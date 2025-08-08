package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.util.enumTilVisningsnavn

enum class ÅrsakAvslag {
    INGEN_AKTIVITET,
    IKKE_I_MÅLGRUPPE,
    INGEN_OVERLAPP_AKTIVITET_MÅLGRUPPE,
    MANGELFULL_DOKUMENTASJON,
    HAR_IKKE_UTGIFTER,
    RETT_TIL_UTSTYRSSTIPEND,
    HAR_IKKE_MERUTGIFTER,
    RETT_TIL_BOSTØTTE,
    ANNET,
}

fun List<ÅrsakAvslag>.formaterListe(): String = joinToString(separator = ", ") { enumTilVisningsnavn(it.name) }
