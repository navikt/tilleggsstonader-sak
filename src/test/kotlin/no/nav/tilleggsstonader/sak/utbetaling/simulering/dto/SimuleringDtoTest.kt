package no.nav.tilleggsstonader.sak.utbetaling.simulering.dto

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.utbetaling.UtbetalingFagområde
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.OppsummeringForPeriode
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.Periode
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.Postering
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.SimuleringDetaljer
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.SimuleringJson
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.Simuleringsresultat
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.PosteringType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class SimuleringDtoTest {
    /**
     * Perioder fra simulering er ikke gruppert per måned
     * Hvis vi har en vedtaksperiode for 2.1-14.1 og 25.1-31.1
     * så vil det være 2 andeler med fom 2.1 og 25.1 som kommer tilbake fra simulering som 2 ulike perioder
     */
    @Test
    fun `skal summere perioder gruppert per måned då simuleringen gir alle perioder i en måned som kan være flere`() {
        assertThat(simuleringsresultat.tilDto(Stønadstype.BARNETILSYN).perioder!!)
            .containsExactlyInAnyOrder(
                OppsummeringForPeriodeDto(
                    måned = YearMonth.of(2024, 1),
                    tidligereUtbetalt = 11,
                    nyUtbetaling = 22,
                    totalEtterbetaling = 33,
                    totalFeilutbetaling = 44,
                ),
                OppsummeringForPeriodeDto(
                    måned = YearMonth.of(2024, 2),
                    tidligereUtbetalt = 101,
                    nyUtbetaling = 202,
                    totalEtterbetaling = 303,
                    totalFeilutbetaling = 404,
                ),
            )
    }

    @Test
    fun `skal vise beløp fra andre stønadstyper gruppert per måned og summert over flere perioder i samme måned`() {
        val simuleringMedAndreStønadstyper =
            simuleringsresultat.copy(
                data =
                    simuleringsresultat.data!!.copy(
                        detaljer =
                            SimuleringDetaljer(
                                gjelderId = "1",
                                datoBeregnet = LocalDate.now(),
                                totalBeløp = 100,
                                perioder =
                                    listOf(
                                        periodeMedPostering(LocalDate.of(2024, 1, 2), "TSTBASISP2-OP", 10), // BARNETILSYN - egen
                                        periodeMedPostering(LocalDate.of(2024, 1, 25), "TSLMASISP2-OP", 5), // LÆREMIDLER - annen
                                        periodeMedPostering(LocalDate.of(2024, 2, 5), "TSBUASIA-OP", 7), // BOUTGIFTER - annen
                                        periodeMedPostering(LocalDate.of(2024, 2, 5), "TSBUASIA-OP", 3), // BOUTGIFTER - annen, samme måned
                                        periodeMedPostering(LocalDate.of(2024, 2, 5), "UKJENT_KLASSEKODE", 1000), // skal ignoreres
                                    ),
                            ),
                    ),
            )

        val perioder = simuleringMedAndreStønadstyper.tilDto(Stønadstype.BARNETILSYN).perioder!!

        val januar = perioder.single { it.måned == YearMonth.of(2024, 1) }
        assertThat(januar.beløpFraAndreStønadstyper)
            .containsExactly(BeløpForStønadstypeDto(Stønadstype.LÆREMIDLER, 5))

        val februar = perioder.single { it.måned == YearMonth.of(2024, 2) }
        assertThat(februar.beløpFraAndreStønadstyper)
            .containsExactly(BeløpForStønadstypeDto(Stønadstype.BOUTGIFTER, 10))
    }

    @Test
    fun `skal vise beløp fra ukjent kilde for klassekoder vi ikke kan mappe til en stønadstype`() {
        val simuleringMedUkjentKilde =
            simuleringsresultat.copy(
                data =
                    simuleringsresultat.data!!.copy(
                        detaljer =
                            SimuleringDetaljer(
                                gjelderId = "1",
                                datoBeregnet = LocalDate.now(),
                                totalBeløp = 100,
                                perioder =
                                    listOf(
                                        periodeMedPostering(LocalDate.of(2024, 1, 2), "TSTBASISP2-OP", 10), // BARNETILSYN - egen
                                        periodeMedPostering(
                                            LocalDate.of(2024, 1, 25),
                                            "TPTPATT-OP",
                                            15,
                                            fagområde = UtbetalingFagområde.TILTAKSPENGER,
                                        ),
                                    ),
                            ),
                    ),
            )

        val perioder = simuleringMedUkjentKilde.tilDto(Stønadstype.BARNETILSYN).perioder!!

        val januar = perioder.single { it.måned == YearMonth.of(2024, 1) }
        assertThat(januar.beløpFraAndreStønadstyper).isEmpty()
        assertThat(januar.beløpFraUkjentKilde)
            .containsExactly(BeløpFraUkjentKildeDto("Tiltakspenger", 15))
    }

    private fun periodeMedPostering(
        dato: LocalDate,
        klassekode: String,
        beløp: Int,
        fagområde: UtbetalingFagområde = UtbetalingFagområde.TILLEGGSSTØNADER,
    ) = Periode(
        fom = dato,
        tom = dato,
        posteringer =
            listOf(
                Postering(
                    fagområde = fagområde,
                    sakId = "1",
                    fom = dato,
                    tom = dato,
                    beløp = beløp,
                    type = PosteringType.YTELSE,
                    klassekode = klassekode,
                ),
            ),
    )

    val simuleringsresultat =
        Simuleringsresultat(
            BehandlingId.random(),
            data =
                SimuleringJson(
                    oppsummeringer =
                        listOf(
                            OppsummeringForPeriode(
                                fom = LocalDate.of(2024, 1, 2),
                                tom = LocalDate.of(2024, 1, 2),
                                tidligereUtbetalt = 10,
                                nyUtbetaling = 20,
                                totalEtterbetaling = 30,
                                totalFeilutbetaling = 40,
                            ),
                            OppsummeringForPeriode(
                                fom = LocalDate.of(2024, 1, 25),
                                tom = LocalDate.of(2024, 1, 25),
                                tidligereUtbetalt = 1,
                                nyUtbetaling = 2,
                                totalEtterbetaling = 3,
                                totalFeilutbetaling = 4,
                            ),
                            OppsummeringForPeriode(
                                fom = LocalDate.of(2024, 2, 5),
                                tom = LocalDate.of(2024, 2, 5),
                                tidligereUtbetalt = 101,
                                nyUtbetaling = 202,
                                totalEtterbetaling = 303,
                                totalFeilutbetaling = 404,
                            ),
                        ),
                    detaljer = SimuleringDetaljer("", LocalDate.now(), 100, emptyList()),
                ),
        )
}
