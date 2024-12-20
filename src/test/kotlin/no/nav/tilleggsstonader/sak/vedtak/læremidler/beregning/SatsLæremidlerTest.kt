package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.førsteOverlappendePeriode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SatsLæremidlerTest {

    @Test
    fun `høyere utdanning 2024`() {
        val periode = Vedtaksperiode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1))
        val sats = finnSatsForStudienivå(
            finnSatsForPeriode(periode),
            Studienivå.HØYERE_UTDANNING,
        )
        assertThat(sats).isEqualTo(875)
    }

    @Test
    fun `høyere utdanning 2023`() {
        val periode = Vedtaksperiode(LocalDate.of(2023, 3, 1), LocalDate.of(2023, 5, 1))
        val sats = finnSatsForStudienivå(
            finnSatsForPeriode(periode),
            Studienivå.HØYERE_UTDANNING,
        )
        assertThat(sats).isEqualTo(822)
    }

    @Test
    fun `videregående utdanning 2024`() {
        val periode = Vedtaksperiode(LocalDate.of(2024, 11, 1), LocalDate.of(2024, 12, 1))
        val sats = finnSatsForStudienivå(
            finnSatsForPeriode(periode),
            Studienivå.VIDEREGÅENDE,
        )
        assertThat(sats).isEqualTo(438)
    }

    @Test
    fun `videregående utdanning 2023`() {
        val periode = Vedtaksperiode(LocalDate.of(2023, 10, 1), LocalDate.of(2023, 12, 1))
        val sats = finnSatsForStudienivå(
            finnSatsForPeriode(periode),
            Studienivå.VIDEREGÅENDE,
        )
        assertThat(sats).isEqualTo(411)
    }

    @Test
    fun `skal kun ha 1 sats som er ubekreftet, for å kunne markere perioder etter nyttår som at de ikke skal utbetales ennå`() {
        val ubekreftedeSatser = satser.filterNot { it.bekreftet }

        assertThat(ubekreftedeSatser).hasSize(1)
    }

    @Test
    fun `skal ikke ha overlappende satser`() {
        val overlappendePeriode = satser.førsteOverlappendePeriode()
        assertThat(overlappendePeriode).isNull()
    }

    @Test
    fun `perioder skal være påfølgende`() {
        satser.sorted().zipWithNext().forEach { (sats1, sats2) ->
            assertThat(sats1.tom.plusDays(1)).isEqualTo(sats2.fom)
        }
    }
}
