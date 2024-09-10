package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.OppsummeringForPeriode
import java.time.LocalDate

data class SimuleringDto(
    val perioder: List<OppsummeringForPeriode>?,
    val ingenEndringIUtbetaling: Boolean,
)

data class SimuleringOppsummering(
    val fom: LocalDate,
    val tom: LocalDate,
    val etterbetaling: Int,
    val feilutbetaling: Int,
    val nesteUtbetaling: NesteUtbetaling?,
)

data class NesteUtbetaling(
    val utbetalingsdato: LocalDate,
    val beløp: Int,
)
