package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype

enum class StønadstypeDvh {
    BARNETILSYN,
    LÆREMIDLER,
    BOUTGIFTER,
    DAGLIG_REISE_TSO,
    DAGLIG_REISE_TSR,
    ;

    companion object {
        fun fraDomene(stønadstype: Stønadstype): StønadstypeDvh =
            when (stønadstype) {
                Stønadstype.BARNETILSYN -> BARNETILSYN
                Stønadstype.LÆREMIDLER -> LÆREMIDLER
                Stønadstype.BOUTGIFTER -> BOUTGIFTER
                Stønadstype.DAGLIG_REISE_TSO -> TODO("Daglig reise er ikke implementert enda")
                Stønadstype.DAGLIG_REISE_TSR -> TODO("Daglig reise er ikke implementert enda")
            }
    }
}
