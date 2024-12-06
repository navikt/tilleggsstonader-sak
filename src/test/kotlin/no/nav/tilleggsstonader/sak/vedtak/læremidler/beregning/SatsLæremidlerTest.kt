package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SatsLæremidlerTest {

    @Test
    fun `høyere utdanning 2024`() {
        val sats = finnSatsForStudienivå(
            VedtaksPeriode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1)),
            Studienivå.HØYERE_UTDANNING,
        )
        assertThat(sats).isEqualTo(875)
    }

    @Test
    fun `høyere utdanning 2023`() {
        val sats = finnSatsForStudienivå(
            VedtaksPeriode(LocalDate.of(2023, 3, 1), LocalDate.of(2023, 5, 1)),
            Studienivå.HØYERE_UTDANNING,
        )
        assertThat(sats).isEqualTo(822)
    }

    @Test
    fun `videregående utdanning 2024`() {
        val sats = finnSatsForStudienivå(
            VedtaksPeriode(LocalDate.of(2024, 11, 1), LocalDate.of(2024, 12, 1)),
            Studienivå.VIDEREGÅENDE,
        )
        assertThat(sats).isEqualTo(438)
    }

    @Test
    fun `videregående utdanning 2023`() {
        val sats = finnSatsForStudienivå(
            VedtaksPeriode(LocalDate.of(2023, 10, 1), LocalDate.of(2023, 12, 1)),
            Studienivå.VIDEREGÅENDE,
        )
        assertThat(sats).isEqualTo(411)
    }
}
