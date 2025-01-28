package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.Fagområde
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.OppsummeringForPeriode
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.Periode
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.Postering
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.SimuleringDetaljer
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.SimuleringJson
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringResponseDto
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.OppsummeringForPeriode as OppsummeringForPeriodeKontrakt
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.Periode as PeriodeKontrakt
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringDetaljer as SimuleringDetaljerKontrakt

object SimuleringKontraktTilDomeneMapper {
    fun map(dto: SimuleringResponseDto): SimuleringJson =
        SimuleringJson(
            oppsummeringer = mapOppsummering(dto.oppsummeringer),
            detaljer = mapDetaljer(dto.detaljer),
        )

    private fun mapDetaljer(detaljer: SimuleringDetaljerKontrakt): SimuleringDetaljer =
        SimuleringDetaljer(
            gjelderId = detaljer.gjelderId,
            datoBeregnet = detaljer.datoBeregnet,
            totalBeløp = detaljer.totalBeløp,
            perioder = mapPerioder(detaljer),
        )

    private fun mapPerioder(detaljer: SimuleringDetaljerKontrakt) =
        detaljer.perioder.map {
            Periode(
                fom = it.fom,
                tom = it.tom,
                posteringer = mapPosteringer(it),
            )
        }

    private fun mapPosteringer(it: PeriodeKontrakt) =
        it.posteringer.map {
            Postering(
                fagområde = Fagområde.valueOf(it.fagområde.name),
                sakId = it.sakId,
                fom = it.fom,
                tom = it.tom,
                beløp = it.beløp,
                type = it.type,
                klassekode = it.klassekode,
            )
        }

    private fun mapOppsummering(oppsummeringer: List<OppsummeringForPeriodeKontrakt>): List<OppsummeringForPeriode> =
        oppsummeringer.map {
            OppsummeringForPeriode(
                fom = it.fom,
                tom = it.tom,
                tidligereUtbetalt = it.tidligereUtbetalt,
                nyUtbetaling = it.nyUtbetaling,
                totalEtterbetaling = it.totalEtterbetaling,
                totalFeilutbetaling = it.totalFeilutbetaling,
            )
        }
}
