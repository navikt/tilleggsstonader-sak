package no.nav.tilleggsstonader.sak.opplysninger.ytelse

import no.nav.tilleggsstonader.kontrakter.ytelse.ResultatKilde
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode

data class HarAktivtVedtakDto(
    val type: TypeYtelsePeriode,
    val harAktivtVedtak: Boolean,
    val resultatKilde: ResultatKilde,
)
