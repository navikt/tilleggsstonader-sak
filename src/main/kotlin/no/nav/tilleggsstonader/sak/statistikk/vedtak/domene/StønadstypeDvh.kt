package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype

enum class StønadstypeDvh {
    BARNETILSYN,
    LÆREMIDLER,
    BOUTGIFTER,
    DAGLIG_REISE_TSO,
    DAGLIG_REISE_TSR,
    REISE_TIL_SAMLING_TSO,
    REISE_TIL_SAMLING_TSR,
    FLYTTING_TSO,
    FLYTTING_TSR,
    ;

    companion object {
        fun fraDomene(stønadstype: Stønadstype): StønadstypeDvh =
            when (stønadstype) {
                Stønadstype.BARNETILSYN -> BARNETILSYN
                Stønadstype.LÆREMIDLER -> LÆREMIDLER
                Stønadstype.BOUTGIFTER -> BOUTGIFTER
                Stønadstype.DAGLIG_REISE_TSO -> DAGLIG_REISE_TSO
                Stønadstype.DAGLIG_REISE_TSR -> DAGLIG_REISE_TSR
                Stønadstype.REISE_TIL_SAMLING_TSO -> REISE_TIL_SAMLING_TSO
                Stønadstype.REISE_TIL_SAMLING_TSR -> REISE_TIL_SAMLING_TSR
                Stønadstype.FLYTTING_TSO -> FLYTTING_TSO
                Stønadstype.FLYTTING_TSR -> FLYTTING_TSR
            }
    }
}
