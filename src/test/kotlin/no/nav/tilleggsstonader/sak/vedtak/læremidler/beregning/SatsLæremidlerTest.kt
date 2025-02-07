package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.førsteOverlappendePeriode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.VedtaksperiodeStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class SatsLæremidlerTest {
    @Test
    fun `høyere utdanning 2024`() {
        val periode =
            Vedtaksperiode(
                id = UUID.randomUUID(),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 2, 1),
                VedtaksperiodeStatus.NY,
            )
        val sats =
            finnSatsForStudienivå(
                finnSatsForPeriode(periode),
                Studienivå.HØYERE_UTDANNING,
            )
        assertThat(sats).isEqualTo(875)
    }

    @Test
    fun `høyere utdanning 2023`() {
        val periode =
            Vedtaksperiode(
                id = UUID.randomUUID(),
                LocalDate.of(2023, 3, 1),
                LocalDate.of(2023, 5, 1),
                VedtaksperiodeStatus.NY,
            )
        val sats =
            finnSatsForStudienivå(
                finnSatsForPeriode(periode),
                Studienivå.HØYERE_UTDANNING,
            )
        assertThat(sats).isEqualTo(822)
    }

    @Test
    fun `videregående utdanning 2024`() {
        val periode =
            Vedtaksperiode(
                id = UUID.randomUUID(),
                LocalDate.of(2024, 11, 1),
                LocalDate.of(2024, 12, 1),
                VedtaksperiodeStatus.NY,
            )
        val sats =
            finnSatsForStudienivå(
                finnSatsForPeriode(periode),
                Studienivå.VIDEREGÅENDE,
            )
        assertThat(sats).isEqualTo(438)
    }

    @Test
    fun `videregående utdanning 2023`() {
        val periode =
            Vedtaksperiode(
                id = UUID.randomUUID(),
                LocalDate.of(2023, 10, 1),
                LocalDate.of(2023, 12, 1),
                VedtaksperiodeStatus.NY,
            )
        val sats =
            finnSatsForStudienivå(
                finnSatsForPeriode(periode),
                Studienivå.VIDEREGÅENDE,
            )
        assertThat(sats).isEqualTo(411)
    }

    @Test
    fun `skal ikke kunne hente sats for en periode som strekker seg over periode for sats`() {
        assertThatThrownBy {
            finnSatsForPeriode(Datoperiode(LocalDate.of(2024, 12, 31), LocalDate.of(2025, 1, 1)))
        }.hasMessageContaining("Finner ikke satser for")
    }

    @Test
    fun `skal kun ha 1 sats som er ubekreftet, for å kunne markere perioder etter nyttår som at de ikke skal utbetales ennå`() {
        val ubekreftedeSatser = satser.filterNot { it.bekreftet }
        val sisteBekreftetSats = satser.filter { it.bekreftet }.maxBy { it.tom }

        assertThat(ubekreftedeSatser).hasSize(1)
        assertThat(ubekreftedeSatser[0].fom).isEqualTo(LocalDate.of(2026, 1, 1))
        assertThat(ubekreftedeSatser[0].tom).isEqualTo(LocalDate.of(2099, 12, 31))
        assertThat(ubekreftedeSatser[0].fom).isEqualTo(sisteBekreftetSats.tom.plusDays(1))
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
