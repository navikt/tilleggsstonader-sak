package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.lagBeregningsresultatForOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.lagBeregningsresultatForPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.detaljerteVedtaksperioder.DetaljertVedtaksperioderReiseTilSamlingMapper.finnDetaljerteVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatReiseTilSamling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class DetaljertVedtaksperioderReiseTilSamlingMapperTest {
    @Test
    fun `ingen perioder gir tom liste`() {
        val resultat = lagVedtak().finnDetaljerteVedtaksperioder()

        assertThat(resultat).isEmpty()
    }

    @Test
    fun `ikke-overlappende perioder beholdes separert`() {
        val vedtak =
            lagVedtak(
                offentligTransport =
                    listOf(
                        lagBeregningsresultatForOffentligTransport(1.januar(2024), 31.januar(2024), beløp = BigDecimal("100")),
                        lagBeregningsresultatForOffentligTransport(1.mars(2024), 31.mars(2024), beløp = BigDecimal("200")),
                    ),
            )

        val resultat = vedtak.finnDetaljerteVedtaksperioder()

        assertThat(resultat).hasSize(2)
        assertThat(resultat[0].fom).isEqualTo(1.mars(2024))
        assertThat(resultat[0].beløp).isEqualByComparingTo(BigDecimal("200"))
        assertThat(resultat[1].fom).isEqualTo(1.januar(2024))
        assertThat(resultat[1].beløp).isEqualByComparingTo(BigDecimal("100"))
    }

    @Test
    fun `overlappende perioder slås sammen med summert beløp og max tom`() {
        val vedtak =
            lagVedtak(
                offentligTransport =
                    listOf(
                        lagBeregningsresultatForOffentligTransport(1.januar(2024), 15.februar(2024), beløp = BigDecimal("300")),
                        lagBeregningsresultatForOffentligTransport(1.februar(2024), 28.februar(2024), beløp = BigDecimal("200")),
                    ),
            )

        val resultat = vedtak.finnDetaljerteVedtaksperioder()

        assertThat(resultat).hasSize(1)
        assertThat(resultat[0].fom).isEqualTo(1.januar(2024))
        assertThat(resultat[0].tom).isEqualTo(28.februar(2024))
        assertThat(resultat[0].beløp).isEqualByComparingTo(BigDecimal("500"))
    }

    @Test
    fun `offentlig transport og privat bil med overlappende perioder slås sammen`() {
        val vedtak =
            lagVedtak(
                offentligTransport =
                    listOf(
                        lagBeregningsresultatForOffentligTransport(1.januar(2024), 31.januar(2024), beløp = BigDecimal("400")),
                    ),
                privatBil =
                    listOf(
                        lagBeregningsresultatForPrivatBil(15.januar(2024), 28.februar(2024), beløp = BigDecimal("100")),
                    ),
            )

        val resultat = vedtak.finnDetaljerteVedtaksperioder()

        assertThat(resultat).hasSize(1)
        assertThat(resultat[0].fom).isEqualTo(1.januar(2024))
        assertThat(resultat[0].tom).isEqualTo(28.februar(2024))
        assertThat(resultat[0].beløp).isEqualByComparingTo(BigDecimal("500"))
    }

    @Test
    fun `resultatet er sortert med nyeste periode først`() {
        val vedtak =
            lagVedtak(
                offentligTransport =
                    listOf(
                        lagBeregningsresultatForOffentligTransport(1.mars(2024), 31.mars(2024), beløp = BigDecimal.ONE),
                        lagBeregningsresultatForOffentligTransport(1.januar(2024), 31.januar(2024), beløp = BigDecimal.ONE),
                    ),
            )

        val resultat = vedtak.finnDetaljerteVedtaksperioder()

        assertThat(resultat.map { it.fom }).containsExactly(1.mars(2024), 1.januar(2024))
    }

    private fun lagVedtak(
        offentligTransport: List<BeregningsresultatOffentligTransport> = emptyList(),
        privatBil: List<BeregningsresultatPrivatBil> = emptyList(),
    ) = InnvilgelseReiseTilSamling(
        beregningsresultat = BeregningsresultatReiseTilSamling(offentligTransport, privatBil),
        vedtaksperioder = emptyList(),
        beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
    )
}
