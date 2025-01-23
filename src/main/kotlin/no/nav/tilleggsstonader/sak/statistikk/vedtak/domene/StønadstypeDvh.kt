package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype

enum class StønadstypeDvh {
    BARNETILSYN,
    LÆREMIDLER,
    ;

    companion object {
        fun fraDomene(stønadstype: Stønadstype): StønadstypeDvh {
            return when (stønadstype) {
                Stønadstype.BARNETILSYN -> BARNETILSYN
                Stønadstype.LÆREMIDLER -> LÆREMIDLER
            }
        }
    }
}
