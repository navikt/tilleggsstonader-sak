package no.nav.tilleggsstonader.sak.felles.domain

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype

fun Stønadstype.gjelderBarn() = when (this) {
    Stønadstype.BARNETILSYN -> true
    Stønadstype.LÆREMIDLER -> false
    else -> error("Har ikke tatt stilling til om stønadstype=$this gjelder barn")
}
