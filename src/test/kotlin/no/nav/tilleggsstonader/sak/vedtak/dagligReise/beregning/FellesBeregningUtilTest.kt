package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FellesBeregningUtilTest {
    val førsteJanuar = LocalDate.of(2025, 1, 1)
    val sjetteJanuar = LocalDate.of(2025, 1, 6)
    val trettendeJanuar = LocalDate.of(2025, 1, 13)
    val tjuendeJanuar = LocalDate.of(2025, 1, 20)
    val tjuesyvendeJanuar = LocalDate.of(2025, 1, 27)

    @Nested
    inner class AntallHverdagerIPeriode {
        @Test
        fun `skal finne 5 hverdager i èn full uke`() {
            val fom = sjetteJanuar
            val tom = LocalDate.of(2025, 1, 12)

            assertThat(antallHverdagerIPeriodeInklusiv(fom, tom)).isEqualTo(5)
        }

        @Test
        fun `skal finne mindre enn 5 dager dersom perioden ikke er en full uke`() {
            val fom = førsteJanuar
            val tom = LocalDate.of(2025, 1, 5)

            assertThat(antallHverdagerIPeriodeInklusiv(fom, tom)).isEqualTo(3)
        }

        @Test
        fun `skal finne antall hverdager over flere uker`() {
            val fom = førsteJanuar
            val tom = LocalDate.of(2025, 1, 18)

            assertThat(antallHverdagerIPeriodeInklusiv(fom, tom)).isEqualTo(13)
        }
    }

    @Nested
    inner class AntallHelgedagerIPeriode {
        @Test
        fun `skal finne 2 helgedager i èn full uke`() {
            val fom = sjetteJanuar
            val tom = LocalDate.of(2025, 1, 12)

            assertThat(antallHelgedagerIPeriodeInklusiv(fom, tom)).isEqualTo(2)
        }

        @Test
        fun `skal finne 1 helgedag om halve helgen er inkludert i perioden`() {
            val fom = førsteJanuar
            val tom = LocalDate.of(2025, 1, 4)

            assertThat(antallHelgedagerIPeriodeInklusiv(fom, tom)).isEqualTo(1)
        }

        @Test
        fun `skal finne antall helgedager over flere uker`() {
            val fom = førsteJanuar
            val tom = LocalDate.of(2025, 1, 18)

            assertThat(antallHelgedagerIPeriodeInklusiv(fom, tom)).isEqualTo(5)
        }
    }

    @Nested
    inner class SplittTilUkerMedHelg {
        @Test
        fun `skal splitte opp i uker og telle antall dager`() {
            val periode = Datoperiode(førsteJanuar, LocalDate.of(2025, 1, 31))

            val forventedeUker =
                listOf(
                    UkeMedAntallDager(
                        fom = førsteJanuar,
                        tom = førsteJanuar.plusDays(4),
                        antallHverdager = 3,
                        antallHelgedager = 2,
                    ),
                    UkeMedAntallDager(
                        fom = sjetteJanuar,
                        tom = sjetteJanuar.plusDays(6),
                        antallHverdager = 5,
                        antallHelgedager = 2,
                    ),
                    UkeMedAntallDager(
                        fom = trettendeJanuar,
                        tom = trettendeJanuar.plusDays(6),
                        antallHverdager = 5,
                        antallHelgedager = 2,
                    ),
                    UkeMedAntallDager(
                        fom = tjuendeJanuar,
                        tom = tjuendeJanuar.plusDays(6),
                        antallHverdager = 5,
                        antallHelgedager = 2,
                    ),
                    UkeMedAntallDager(
                        fom = tjuesyvendeJanuar,
                        tom = LocalDate.of(2025, 1, 31),
                        antallHverdager = 5,
                        antallHelgedager = 0,
                    ),
                )

            val result = periode.splitPerUkeMedHelg()

            assertThat(result).isEqualTo(forventedeUker)
        }
    }
}
