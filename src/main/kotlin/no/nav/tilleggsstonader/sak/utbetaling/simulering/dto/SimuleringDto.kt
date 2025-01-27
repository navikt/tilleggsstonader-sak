package no.nav.tilleggsstonader.sak.utbetaling.simulering.dto

import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.OppsummeringForPeriode
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.Simuleringsresultat
import java.time.LocalDate
import java.time.YearMonth

data class SimuleringDto(
    val perioder: List<OppsummeringForPeriodeDto>?,
    val ingenEndringIUtbetaling: Boolean,
    val oppsummering: SimuleringOppsummering?,
)

data class SimuleringOppsummering(
    val fom: LocalDate,
    val tom: LocalDate,
    val etterbetaling: Int,
    val feilutbetaling: Int,
)

data class OppsummeringForPeriodeDto(
    val måned: YearMonth,
    @Deprecated("bruk måned når frontend tatt den i bruk")
    val fom: LocalDate,
    val tidligereUtbetalt: Int,
    val nyUtbetaling: Int,
    val totalEtterbetaling: Int,
    val totalFeilutbetaling: Int,
)

fun Simuleringsresultat.tilDto(): SimuleringDto =
    SimuleringDto(
        perioder =
            this.data
                ?.oppsummeringer
                ?.map { it.tilDto() }
                ?.summerPerMåned(),
        ingenEndringIUtbetaling = this.ingenEndringIUtbetaling,
        oppsummering = lagSimuleringOppsummering(this),
    )

private fun OppsummeringForPeriode.tilDto(): OppsummeringForPeriodeDto {
    val måned = YearMonth.from(fom)
    require(måned == YearMonth.from(tom))
    return OppsummeringForPeriodeDto(
        måned = måned,
        fom = fom,
        tidligereUtbetalt = tidligereUtbetalt,
        nyUtbetaling = nyUtbetaling,
        totalEtterbetaling = totalEtterbetaling,
        totalFeilutbetaling = totalFeilutbetaling,
    )
}

private fun List<OppsummeringForPeriodeDto>.summerPerMåned() =
    groupBy { it.måned }
        .mapValues {
            it.value.reduce { acc, periode ->
                acc.copy(
                    tidligereUtbetalt = acc.tidligereUtbetalt + periode.tidligereUtbetalt,
                    nyUtbetaling = acc.nyUtbetaling + periode.nyUtbetaling,
                    totalEtterbetaling = acc.totalEtterbetaling + periode.totalEtterbetaling,
                    totalFeilutbetaling = acc.totalFeilutbetaling + periode.totalFeilutbetaling,
                )
            }
        }.values
        .toList()

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
