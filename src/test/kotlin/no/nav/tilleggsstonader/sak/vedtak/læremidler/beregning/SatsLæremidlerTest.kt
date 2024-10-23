package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.YearMonth

class SatsLæremidlerTest {

    @Test
    fun `høyere utdanning 2024`() {
        val sats = finnSatsForStudienivå(YearMonth.of(2024, 1), Studienivå.HØYERE_UTDANNING)
        assertThat(sats).isEqualTo(875)
    }

    @Test
    fun `høyere utdanning 2023`() {
        val sats = finnSatsForStudienivå(YearMonth.of(2023, 2), Studienivå.HØYERE_UTDANNING)
        assertThat(sats).isEqualTo(822)
    }

    @Test
    fun `videregående utdanning 2024`() {
        val sats = finnSatsForStudienivå(YearMonth.of(2024, 11), Studienivå.VIDEREGÅENDE)
        assertThat(sats).isEqualTo(438)
    }

    @Test
    fun `videregående utdanning 2023`() {
        val sats = finnSatsForStudienivå(YearMonth.of(2023, 12), Studienivå.VIDEREGÅENDE)
        assertThat(sats).isEqualTo(411)
    }
}
