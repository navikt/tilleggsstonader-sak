package no.nav.tilleggsstonader.sak.utbetaling.simulering.dto

import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.Simuleringsresultat
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

fun Simuleringsresultat.tilDto(): SimuleringDto {
    return SimuleringDto(
        perioder = this.data?.oppsummeringer,
        ingenEndringIUtbetaling = this.ingenEndringIUtbetaling,
        oppsummering = lagSimuleringOppsummering(this),
    )
}

private fun lagSimuleringOppsummering(simulering: Simuleringsresultat): SimuleringOppsummering? {
    if (simulering.data == null) {
        return null
    }

    return SimuleringOppsummering(
        fom = simulering.data.oppsummeringer.minOf { it.fom },
        tom = simulering.data.oppsummeringer.maxOf { it.tom },
        etterbetaling = simulering.data.oppsummeringer.sumOf { it.totalEtterbetaling },
        feilutbetaling = simulering.data.oppsummeringer.sumOf { it.totalFeilutbetaling },
    )
}
