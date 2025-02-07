package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningV1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class UtgiftBeregningTest {
    @Nested
    inner class SplitFraRevurderFra {
        val utgiftJanuar =
            UtgiftBeregning(
                fom = YearMonth.of(2025, 1),
                tom = YearMonth.of(2025, 1),
                utgift = 1000,
            )
        val utgiftFebruar =
            UtgiftBeregning(
                fom = YearMonth.of(2025, 2),
                tom = YearMonth.of(2025, 2),
                utgift = 1000,
            )
        val utgiftJanuarTilFebruar =
            UtgiftBeregning(
                fom = YearMonth.of(2025, 1),
                tom = YearMonth.of(2025, 2),
                utgift = 1000,
            )
        val utgifter = listOf(utgiftJanuarTilFebruar)

        @Test
        fun `skal splitte på revurder fra`() {
            val revurderFra = LocalDate.of(2025, 2, 1)

            val forventetUtgifter =
                listOf(
                    utgiftJanuar,
                    utgiftFebruar,
                )

            assertThat(utgifter.splitFraRevurderFra(revurderFra)).isEqualTo(forventetUtgifter)
        }

        @Test
        fun `skal ikke splitte når ingen revurder fra`() {
            assertThat(utgifter.splitFraRevurderFra(null)).isEqualTo(utgifter)
        }
    }
}
