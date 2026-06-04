package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.libs.utils.dato.april
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.lagBeregningsresultatMåned
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BoutgifterBeregningServiceJusterBeregnFraTest {
    private val periode19Jan =
        lagBeregningsresultatMåned(
            fom = 19 januar 2026,
            tom = 18 februar 2026,
            utgifter = mapOf(TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG to emptyList()),
        )
    private val periode19Feb =
        lagBeregningsresultatMåned(
            fom = 19 februar 2026,
            tom = 18 mars 2026,
            utgifter = mapOf(TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG to emptyList()),
        )
    private val periode19Mar =
        lagBeregningsresultatMåned(
            fom = 19 mars 2026,
            tom = 18 april 2026,
            utgifter = mapOf(TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG to emptyList()),
        )

    private fun innvilgelseMedPerioder(vararg perioder: BeregningsresultatForLøpendeMåned) =
        InnvilgelseBoutgifter(
            beregningsresultat = BeregningsresultatBoutgifter(perioder.toList()),
            vedtaksperioder = listOf(vedtaksperiode(fom = 19 januar 2026, tom = 18 april 2026)),
            beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
        )

    @Nested
    inner class `uten forrige vedtak` {
        @Test
        fun `returnerer beregnFra uendret når forrigeVedtak er null`() {
            val juster = BoutgifterBeregningService.justerBeregnFra(null)

            assertThat(juster(1 januar 2026)).isEqualTo(1 januar 2026)
            assertThat(juster(19 mars 2026)).isEqualTo(19 mars 2026)
        }
    }

    @Nested
    inner class `med forrige vedtak` {
        private val forrigeVedtak = innvilgelseMedPerioder(periode19Jan, periode19Feb, periode19Mar)

        @Test
        fun `returnerer fom av perioden som inneholder beregnFra`() {
            val juster = BoutgifterBeregningService.justerBeregnFra(forrigeVedtak)

            // beregnFra 1. mars treffer perioden 19.feb–18.mar → juster til 19.feb
            assertThat(juster(1 mars 2026)).isEqualTo(19 februar 2026)
        }

        @Test
        fun `returnerer fom uendret når beregnFra er første dag i perioden`() {
            val juster = BoutgifterBeregningService.justerBeregnFra(forrigeVedtak)

            assertThat(juster(19 januar 2026)).isEqualTo(19 januar 2026)
        }

        @Test
        fun `returnerer beregnFra uendret når ingen periode inneholder beregnFra`() {
            val juster = BoutgifterBeregningService.justerBeregnFra(forrigeVedtak)

            // 19. april er etter alle periodene
            assertThat(juster(19 april 2026)).isEqualTo(19 april 2026)
        }
    }
}
