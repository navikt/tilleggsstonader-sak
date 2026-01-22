package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FellesBeregningUtilTest {
    val førsteJanuar = LocalDate.of(2025, 1, 1)
    val sjetteJanuar = LocalDate.of(2025, 1, 6)

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
}
