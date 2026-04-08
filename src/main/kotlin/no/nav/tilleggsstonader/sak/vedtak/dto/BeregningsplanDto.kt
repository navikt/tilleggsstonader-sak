package no.nav.tilleggsstonader.sak.vedtak.dto

import no.nav.tilleggsstonader.sak.vedtak.BeregningPlan
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import java.time.LocalDate

data class BeregningsplanDto(
    val omfang: Beregningsomfang,
    val fraDato: LocalDate? = null,
)

fun BeregningPlan.tilDto() =
    BeregningsplanDto(
        omfang = omfang,
        fraDato = fraDato,
    )
