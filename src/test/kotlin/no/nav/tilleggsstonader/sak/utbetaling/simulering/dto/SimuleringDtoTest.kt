package no.nav.tilleggsstonader.sak.utbetaling.simulering.dto

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.OppsummeringForPeriode
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.SimuleringDetaljer
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.SimuleringJson
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.Simuleringsresultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class SimuleringDtoTest {
    /**
     * Perioder fra simulering er ikke gruppert per måned
     * Hvis vi har en stønadsperiode for 2.1-14.1 og 25.1-31.1
     * så vil det være 2 andeler med fom 2.1 og 25.1 som kommer tilbake fra simulering som 2 ulike perioder
     */
    @Test
    fun `skal summere perioder gruppert per måned då simuleringen gir alle perioder i en måned som kan være flere`() {
        assertThat(simuleringsresultat.tilDto().perioder!!)
            .containsExactlyInAnyOrder(
                OppsummeringForPeriodeDto(
                    måned = YearMonth.of(2024, 1),
                    fom = LocalDate.of(2024, 1, 2),
                    tidligereUtbetalt = 11,
                    nyUtbetaling = 22,
                    totalEtterbetaling = 33,
                    totalFeilutbetaling = 44,
                ),
                OppsummeringForPeriodeDto(
                    måned = YearMonth.of(2024, 2),
                    fom = LocalDate.of(2024, 2, 5),
                    tidligereUtbetalt = 101,
                    nyUtbetaling = 202,
                    totalEtterbetaling = 303,
                    totalFeilutbetaling = 404,
                ),
            )
    }

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
