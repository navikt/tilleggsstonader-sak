package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.OppsummeringForPeriode
import java.time.LocalDate

data class SimuleringDto(
    val perioder: List<OppsummeringForPeriode>?,
    val ingenEndringIUtbetaling: Boolean,
    val oppsummering: SimuleringOppsummering?,
)

data class SimuleringOppsummering(
    val fom: LocalDate,
    val tom: LocalDate,
    val etterbetaling: Int,
    val feilutbetaling: Int,
)
