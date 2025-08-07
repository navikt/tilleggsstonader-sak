package no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UtgiftOffentligTransportTest {
    @Test
    fun `Sjekker at 30-dagersperiode er 30 dager`() {
        val perioder = Datoperiode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 4, 15))
        val res = perioder.splitPer30DagersPerioder { fom, tom -> Datoperiode(fom, tom) }

        assertThat(res).isEqualTo(
            listOf(
                Datoperiode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 30)),
                Datoperiode(LocalDate.of(2025, 1, 31), LocalDate.of(2025, 3, 1)),
                Datoperiode(LocalDate.of(2025, 3, 2), LocalDate.of(2025, 3, 31)),
                Datoperiode(LocalDate.of(2025, 4, 1), LocalDate.of(2025, 4, 15)),
            ),
        )
    }
}
