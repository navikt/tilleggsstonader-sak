package no.nav.tilleggsstonader.sak.felles.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype

fun Stønadstype.gjelderBarn() =
    when (this) {
        Stønadstype.BARNETILSYN -> true
        Stønadstype.LÆREMIDLER -> false
        Stønadstype.BOUTGIFTER -> false
        Stønadstype.DAGLIG_REISE_TSO -> false
        Stønadstype.DAGLIG_REISE_TSR -> false
    }
