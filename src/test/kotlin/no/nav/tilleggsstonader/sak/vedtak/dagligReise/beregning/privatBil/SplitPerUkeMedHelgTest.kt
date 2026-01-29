package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SplitPerUkeMedHelgTest {
    val førsteJanuar = LocalDate.of(2025, 1, 1)
    val sjetteJanuar = LocalDate.of(2025, 1, 6)
    val trettendeJanuar = LocalDate.of(2025, 1, 13)
    val tjuendeJanuar = LocalDate.of(2025, 1, 20)
    val tjuesyvendeJanuar = LocalDate.of(2025, 1, 27)

    @Test
    fun `skal splitte opp i uker og telle antall dager`() {
        val periode = Datoperiode(førsteJanuar, LocalDate.of(2025, 1, 31))

        val forventedeUker =
            listOf(
                Datoperiode(
                    fom = førsteJanuar,
                    tom = førsteJanuar.plusDays(4),
                ),
                Datoperiode(
                    fom = sjetteJanuar,
                    tom = sjetteJanuar.plusDays(6),
                ),
                Datoperiode(
                    fom = trettendeJanuar,
                    tom = trettendeJanuar.plusDays(6),
                ),
                Datoperiode(
                    fom = tjuendeJanuar,
                    tom = tjuendeJanuar.plusDays(6),
                ),
                Datoperiode(
                    fom = tjuesyvendeJanuar,
                    tom = LocalDate.of(2025, 1, 31),
                ),
            )

        val result = periode.splitPerUkeMedHelg()

        assertThat(result).isEqualTo(forventedeUker)
    }
}
