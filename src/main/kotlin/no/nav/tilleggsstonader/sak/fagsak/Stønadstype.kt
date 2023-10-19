package no.nav.tilleggsstonader.sak.fagsak

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype as StønadstypeKontrakter

enum class Stønadstype {
    BARNETILSYN,
}
fun StønadstypeKontrakter.tilInternType(): Stønadstype = when (this) {
    StønadstypeKontrakter.BARNETILSYN -> Stønadstype.BARNETILSYN
}
