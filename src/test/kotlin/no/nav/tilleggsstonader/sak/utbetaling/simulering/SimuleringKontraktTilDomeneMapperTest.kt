package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.Fagområde
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.OppsummeringForPeriode
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.Periode
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.SimuleringDetaljer
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.PosteringType
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringResponseDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.Fagområde as FagområdeKontrakt
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.OppsummeringForPeriode as OppsummeringForPeriodeKontrakt
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.Periode as PeriodeKontrakt
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.Postering as PosteringKontrakt
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringDetaljer as SimuleringDetaljerKontrakt

class SimuleringKontraktTilDomeneMapperTest {

    val oppsummeringFom = LocalDate.of(2023, 1, 1)
    val oppsummeringTom = LocalDate.of(2023, 1, 2)
    val periodeFom = LocalDate.of(2023, 1, 3)
    val periodeTom = LocalDate.of(2023, 1, 4)
    val posteringFom = LocalDate.of(2023, 1, 5)
    val posteringTom = LocalDate.of(2023, 1, 6)
    val datoBeregnet = LocalDate.of(2023, 1, 7)

    @Test
    fun `should map SimuleringResponseDto to SimuleringJson correctly`() {
        val simuleringResponseDto = opprettSimuleringResponseDto()

        val simuleringJson = SimuleringKontraktTilDomeneMapper.map(simuleringResponseDto)

        assertOppsummeringer(simuleringJson.oppsummeringer)
        assertDetaljer(simuleringJson.detaljer)
    }

    private fun assertOppsummeringer(oppsummeringer: List<OppsummeringForPeriode>) {
        assertThat(oppsummeringer).hasSize(1)
        assertThat(oppsummeringer[0].fom).isEqualTo(oppsummeringFom)
        assertThat(oppsummeringer[0].tom).isEqualTo(oppsummeringTom)
        assertThat(oppsummeringer[0].tidligereUtbetalt).isEqualTo(1000)
        assertThat(oppsummeringer[0].nyUtbetaling).isEqualTo(500)
        assertThat(oppsummeringer[0].totalEtterbetaling).isEqualTo(1500)
        assertThat(oppsummeringer[0].totalFeilutbetaling).isEqualTo(40)
    }

    private fun assertDetaljer(detaljer: SimuleringDetaljer) {
        assertThat(detaljer.gjelderId).isEqualTo("abc123")
        assertThat(detaljer.datoBeregnet).isEqualTo(datoBeregnet)
        assertThat(detaljer.totalBeløp).isEqualTo(5000)

        assertPerioder(detaljer.perioder)
    }

    private fun assertPerioder(perioder: List<Periode>) {
        assertThat(perioder).hasSize(1)
        val periode = perioder[0]
        assertThat(periode.fom).isEqualTo(periodeFom)
        assertThat(periode.tom).isEqualTo(periodeTom)

        assertPosteringer(periode)
    }

    private fun assertPosteringer(periode: Periode) {
        assertThat(periode.posteringer).hasSize(1)
        val postering = periode.posteringer[0]
        assertThat(postering.fagområde).isEqualTo(Fagområde.TILLEGGSSTØNADER)
        assertThat(postering.sakId).isEqualTo("1234")
        assertThat(postering.beløp).isEqualTo(1000)
    }

    private fun opprettSimuleringResponseDto(): SimuleringResponseDto {
        val oppsummeringDto = OppsummeringForPeriodeKontrakt(
            fom = oppsummeringFom,
            tom = oppsummeringTom,
            tidligereUtbetalt = 1000,
            nyUtbetaling = 500,
            totalEtterbetaling = 1500,
            totalFeilutbetaling = 40,
        )
        val periodeDto = PeriodeKontrakt(
            fom = periodeFom,
            tom = periodeTom,
            posteringer = listOf(
                PosteringKontrakt(
                    fagområde = FagområdeKontrakt.TILLEGGSSTØNADER,
                    sakId = "1234",
                    fom = posteringFom,
                    tom = posteringTom,
                    beløp = 1000,
                    type = PosteringType.TREKK,
                    klassekode = "someKlassekode",
                ),
            ),
        )
        val detaljerDto = SimuleringDetaljerKontrakt(
            gjelderId = "abc123",
            datoBeregnet = datoBeregnet,
            totalBeløp = 5000,
            perioder = listOf(periodeDto),
        )
        val simuleringResponseDto = SimuleringResponseDto(
            oppsummeringer = listOf(oppsummeringDto),
            detaljer = detaljerDto,
        )
        return simuleringResponseDto
    }
}
