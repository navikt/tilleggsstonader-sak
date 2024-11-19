package no.nav.tilleggsstonader.sak.vedtak.domain

enum class ÅrsakAvslag {
    INGEN_AKTIVITET,
    IKKE_I_MÅLGRUPPE,
    INGEN_OVERLAPP_AKTIVITET_MÅLGRUPPE,
    MANGELFULL_DOKUMENTASJON,
    ANNET,
    ;

    data class Wrapper(
        val årsaker: List<ÅrsakAvslag>,
    )
}
