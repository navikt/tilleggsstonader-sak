package no.nav.tilleggsstonader.sak.felles.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype

fun Stønadstype.gjelderBarn() =
    when (this) {
        Stønadstype.BARNETILSYN -> true
        Stønadstype.LÆREMIDLER -> false
        Stønadstype.BOUTGIFTER -> false
        Stønadstype.DAGLIG_REISE_TSO -> false
        Stønadstype.DAGLIG_REISE_TSR -> false
        Stønadstype.REISE_TIL_SAMLING_TSO -> false
        Stønadstype.REISE_TIL_SAMLING_TSR -> false
        Stønadstype.FLYTTING_TSO -> false
        Stønadstype.FLYTTING_TSR -> false
        Stønadstype.STØTTE_TIL_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO -> false
        Stønadstype.STØTTE_TIL_REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR -> false
    }
