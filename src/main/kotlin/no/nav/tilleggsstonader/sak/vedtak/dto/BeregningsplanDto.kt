package no.nav.tilleggsstonader.sak.vedtak.dto

import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import java.time.LocalDate

data class BeregningsplanDto(
    val omfang: Beregningsomfang,
    val fraDato: LocalDate? = null,
)

fun Beregningsplan.tilDto() =
    BeregningsplanDto(
        omfang = omfang,
        fraDato = fraDato,
    )
