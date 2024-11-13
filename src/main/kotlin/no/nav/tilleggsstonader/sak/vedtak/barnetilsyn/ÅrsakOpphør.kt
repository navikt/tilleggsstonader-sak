package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

enum class ÅrsakOpphør {
    ENDRING_AKTIVITET,
    ENDRING_MÅLGRUPPE,
    ENDRING_UTGIFTER,
    ANNET,
    ;

    data class Wrapper(
        val årsaker: List<ÅrsakOpphør>,
    )
}
