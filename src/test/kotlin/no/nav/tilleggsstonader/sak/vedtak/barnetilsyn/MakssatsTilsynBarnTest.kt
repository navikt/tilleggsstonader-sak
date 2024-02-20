package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.YearMonth

class MakssatsTilsynBarnTest {

    @Test
    fun `skal mappe verdier for 2024`() {
        val verdier = mapOf(
            1 to 4650,
            2 to 6066,
            3 to 6875,
        )
        val måneder = listOf(YearMonth.of(2024, 1), YearMonth.of(2024, 12))
        validerVerdier(måneder, verdier)
    }

    @Test
    fun `skal mappe verdier for 2023 - juli-des`() {
        val verdier = mapOf(
            1 to 4480,
            2 to 5844,
            3 to 6623,
        )
        val måneder = listOf(YearMonth.of(2023, 7), YearMonth.of(2023, 12))
        validerVerdier(måneder, verdier)
    }

    @Test
    fun `skal mappe verdier for 2023 - jan-juni`() {
        val verdier = mapOf(
            1 to 4369,
            2 to 5700,
            3 to 6460,
        )
        val måneder = listOf(YearMonth.of(2023, 1), YearMonth.of(2023, 6))
        validerVerdier(måneder, verdier)
    }

    fun validerVerdier(måneder: List<YearMonth>, verdier: Map<Int, Int>) {
        måneder.forEach { måned ->
            verdier.entries.forEach { (antallBarn, makssats) ->
                assertThat(finnMakssats(måned, antallBarn)).isEqualTo(makssats)
            }
            assertThat(finnMakssats(måned, 4)).isEqualTo(verdier[3])
        }
    }
}
