package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.førsteOverlappendePeriode
import no.nav.tilleggsstonader.libs.utils.dato.desember
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SatsLæremidlerTest {
    private val satsLæremidlerService = SatsLæremidlerService(SatsLæremidlerProvider())

    @Test
    fun `høyere utdanning 2024`() {
        val periode =
            vedtaksperiode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 2, 1),
            )
        val sats = satsLæremidlerService.finnSatsForPeriode(periode).finnSatsForStudienivå(Studienivå.HØYERE_UTDANNING)
        assertThat(sats).isEqualTo(875)
    }

    @Test
    fun `høyere utdanning 2023`() {
        val periode =
            vedtaksperiode(
                fom = LocalDate.of(2023, 3, 1),
                tom = LocalDate.of(2023, 5, 1),
            )
        val sats = satsLæremidlerService.finnSatsForPeriode(periode).finnSatsForStudienivå(Studienivå.HØYERE_UTDANNING)
        assertThat(sats).isEqualTo(822)
    }

    @Test
    fun `videregående utdanning 2024`() {
        val periode =
            vedtaksperiode(
                fom = LocalDate.of(2024, 11, 1),
                tom = LocalDate.of(2024, 12, 1),
            )
        val sats = satsLæremidlerService.finnSatsForPeriode(periode).finnSatsForStudienivå(Studienivå.VIDEREGÅENDE)
        assertThat(sats).isEqualTo(438)
    }

    @Test
    fun `videregående utdanning 2023`() {
        val periode =
            vedtaksperiode(
                fom = LocalDate.of(2023, 10, 1),
                tom = LocalDate.of(2023, 12, 1),
            )
        val sats = satsLæremidlerService.finnSatsForPeriode(periode).finnSatsForStudienivå(Studienivå.VIDEREGÅENDE)
        assertThat(sats).isEqualTo(411)
    }

    @Test
    fun `skal hente sats for en periode som strekker seg mellom to sats perioder`() {
        val periode =
            Datoperiode(
                fom = 31 desember 2024,
                tom = 1 januar 2025,
            )
        val sats =
            satsLæremidlerService
                .finnSatsForPeriode(periode)
                .finnSatsForStudienivå(Studienivå.VIDEREGÅENDE)
        assertThat(sats).isEqualTo(438)
    }

    @Test
    fun `skal kun ha 1 sats som er ubekreftet, for å kunne markere perioder etter nyttår som at de ikke skal utbetales ennå`() {
        val ubekreftedeSatser = satsLæremidlerService.alleSatser().filterNot { it.bekreftet }
        val sisteBekreftetSats = satsLæremidlerService.alleSatser().filter { it.bekreftet }.maxBy { it.tom }

        assertThat(ubekreftedeSatser).hasSize(1)
        assertThat(ubekreftedeSatser[0].fom).isEqualTo(LocalDate.of(2027, 1, 1))
        assertThat(ubekreftedeSatser[0].tom).isEqualTo(LocalDate.of(2099, 12, 31))
        assertThat(ubekreftedeSatser[0].fom).isEqualTo(sisteBekreftetSats.tom.plusDays(1))
    }

    @Test
    fun `skal ikke ha overlappende satser`() {
        val overlappendePeriode = satsLæremidlerService.alleSatser().førsteOverlappendePeriode()
        assertThat(overlappendePeriode).isNull()
    }

    @Test
    fun `perioder skal være påfølgende`() {
        satsLæremidlerService.alleSatser().sorted().zipWithNext().forEach { (sats1, sats2) ->
            assertThat(sats1.tom.plusDays(1)).isEqualTo(sats2.fom)
        }
    }
}
