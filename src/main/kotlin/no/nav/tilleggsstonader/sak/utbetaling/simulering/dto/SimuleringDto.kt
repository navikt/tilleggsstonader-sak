package no.nav.tilleggsstonader.sak.utbetaling.simulering.dto

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.utbetaling.UtbetalingFagområde
import no.nav.tilleggsstonader.sak.utbetaling.simulering.PosteringStønadstypeMapper
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.OppsummeringForPeriode
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.Simuleringsresultat
import java.time.LocalDate
import java.time.YearMonth

data class SimuleringDto(
    val perioder: List<OppsummeringForPeriodeDto>?,
    val ingenEndringIUtbetaling: Boolean,
    val oppsummering: SimuleringOppsummering?,
    val varsel: String? = null,
)

data class SimuleringOppsummering(
    val fom: LocalDate,
    val tom: LocalDate,
    val etterbetaling: Int,
    val feilutbetaling: Int,
)

data class OppsummeringForPeriodeDto(
    val måned: YearMonth,
    val tidligereUtbetalt: Int,
    val nyUtbetaling: Int,
    val totalEtterbetaling: Int,
    val totalFeilutbetaling: Int,
    val beløpFraAndreStønadstyper: List<BeløpForStønadstypeDto> = emptyList(),
    val beløpFraUkjentKilde: List<BeløpFraUkjentKildeDto> = emptyList(),
)

/**
 * Viser at deler av beløpet i en [OppsummeringForPeriodeDto] tilhører en annen stønadstype enn
 * den behandlingen selv gjelder for. Dette kan forekomme fordi simuleringen viser alle posteringer
 * på samme fagområde, uavhengig av hvilken fagsak/stønadstype de tilhører.
 */
data class BeløpForStønadstypeDto(
    val stønadstype: Stønadstype,
    val beløp: Int,
)

/**
 * Viser at deler av beløpet i en [OppsummeringForPeriodeDto] kommer fra en klassekode vi ikke har
 * noe forhold til, f.eks. tiltakspenger som kan motposteres mot vårt gamle fagområde og dermed
 * dukke opp i simuleringen for fagsaker som utbetaler med gammelt fagområde.
 */
data class BeløpFraUkjentKildeDto(
    val kilde: String,
    val beløp: Int,
)

fun Simuleringsresultat.tilDto(egenStønadstype: Stønadstype): SimuleringDto {
    val perioder = this.data?.detaljer?.perioder
    val beløpFraAndreStønadstyperPerMåned =
        perioder
            ?.let { PosteringStønadstypeMapper.beløpFraAndreStønadstyperPerMåned(it, egenStønadstype) }
            ?: emptyMap()
    val beløpFraUkjentKildePerMåned =
        perioder
            ?.let { PosteringStønadstypeMapper.beløpFraUkjentKildePerMåned(it) }
            ?: emptyMap()

    return SimuleringDto(
        perioder =
            this.data
                ?.oppsummeringer
                ?.map { it.tilDto() }
                ?.summerPerMåned()
                ?.leggTilBeløpFraAndreStønadstyper(beløpFraAndreStønadstyperPerMåned)
                ?.leggTilBeløpFraUkjentKilde(beløpFraUkjentKildePerMåned),
        ingenEndringIUtbetaling = this.ingenEndringIUtbetaling,
        oppsummering = lagSimuleringOppsummering(this),
        varsel =
            "Forrige vedtak har enda ikke blitt registrert i økonomisystemet. Simuleringen kan derfor være unøyaktig"
                .takeIf { finnesUtbetalingerPåFagsområdeSomIkkeErRegistrert },
    )
}

/**
 * På grunn av at vi summerer perioder per måned, så må fom og tom være i samme måned.
 * Hvis ikke burde man vurdere å endre til å bruke fom/tom i stedet
 */
private fun OppsummeringForPeriode.tilDto(): OppsummeringForPeriodeDto {
    val måned = YearMonth.from(fom)
    require(måned == YearMonth.from(tom))
    return OppsummeringForPeriodeDto(
        måned = måned,
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

/**
 * Kobler på beløp fra andre stønadstyper per måned. Gjøres etter [summerPerMåned] for å unngå at
 * beløpet blir summert flere ganger dersom en måned består av flere perioder fra simuleringen.
 */
private fun List<OppsummeringForPeriodeDto>.leggTilBeløpFraAndreStønadstyper(
    beløpFraAndreStønadstyperPerMåned: Map<YearMonth, Map<Stønadstype, Int>>,
): List<OppsummeringForPeriodeDto> =
    map { periode ->
        val beløpFraAndreStønadstyper =
            beløpFraAndreStønadstyperPerMåned[periode.måned]
                ?.map { (stønadstype, beløp) -> BeløpForStønadstypeDto(stønadstype, beløp) }
                ?: emptyList()
        periode.copy(beløpFraAndreStønadstyper = beløpFraAndreStønadstyper)
    }

/**
 * Kobler på beløp fra ukjente kilder per måned, se [BeløpFraUkjentKildeDto]. Gjøres etter
 * [summerPerMåned] for å unngå at beløpet blir summert flere ganger dersom en måned består av
 * flere perioder fra simuleringen.
 */
private fun List<OppsummeringForPeriodeDto>.leggTilBeløpFraUkjentKilde(
    beløpFraUkjentKildePerMåned: Map<YearMonth, Map<UtbetalingFagområde, Int>>,
): List<OppsummeringForPeriodeDto> =
    map { periode ->
        val beløpFraUkjentKilde =
            beløpFraUkjentKildePerMåned[periode.måned]
                ?.map { (fagområde, beløp) -> BeløpFraUkjentKildeDto(fagområde.tilVisningsnavn(), beløp) }
                ?: emptyList()
        periode.copy(beløpFraUkjentKilde = beløpFraUkjentKilde)
    }

/**
 * Fagområder vi kan få posteringer fra som vi ikke har noe forhold til, f.eks. fordi de kan
 * motposteres mot vårt gamle fagområde. Se [UtbetalingFagområde.TILTAKSPENGER].
 */
private fun UtbetalingFagområde.tilVisningsnavn(): String =
    when (this) {
        UtbetalingFagområde.TILTAKSPENGER -> "Tiltakspenger"
        else -> this.name
    }

private fun lagSimuleringOppsummering(simulering: Simuleringsresultat): SimuleringOppsummering? {
    if (simulering.data == null || simulering.data.oppsummeringer.isEmpty()) {
        return null
    }

    return SimuleringOppsummering(
        fom = simulering.data.oppsummeringer.minOf { it.fom },
        tom = simulering.data.oppsummeringer.maxOf { it.tom },
        etterbetaling = simulering.data.oppsummeringer.sumOf { it.totalEtterbetaling },
        feilutbetaling = simulering.data.oppsummeringer.sumOf { it.totalFeilutbetaling },
    )
}
