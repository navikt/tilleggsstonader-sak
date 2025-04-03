package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningsgrunnlagLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.vedtaksperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerBeregnUtil.splittTilLøpendeMåneder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class LæremidlerBeregnUtilTest {
    private val førsteJan2024 = LocalDate.of(2024, 1, 1)
    private val sisteJan2024 = LocalDate.of(2024, 1, 31)

    @Test
    fun `skal håndtere vedtaksperiode som løper over 2 år`() {
        val vedtaksperioder =
            listOf(
                vedtaksperiodeBeregningsgrunnlag(
                    fom = LocalDate.of(2024, 12, 5),
                    tom = LocalDate.of(2025, 1, 4),
                ),
            )
        val perioder = vedtaksperioder.splittTilLøpendeMåneder()
        assertThat(perioder).hasSize(2)

        with(perioder[0]) {
            assertThat(fom).isEqualTo(LocalDate.of(2024, 12, 5))
            assertThat(tom).isEqualTo(LocalDate.of(2024, 12, 31))
            assertThat(this.vedtaksperioder).containsExactly(
                VedtaksperiodeInnenforLøpendeMåned(LocalDate.of(2024, 12, 5), LocalDate.of(2024, 12, 31)),
            )
        }
        with(perioder[1]) {
            assertThat(fom).isEqualTo(LocalDate.of(2025, 1, 1))
            assertThat(tom).isEqualTo(LocalDate.of(2025, 1, 31))
            assertThat(this.vedtaksperioder).containsExactly(
                VedtaksperiodeInnenforLøpendeMåned(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 4)),
            )
        }
    }

    @Nested
    inner class Helg {
        @Test
        fun `skal lage løpende måned for vedtaksperiode som kun er under en helg`() {
            val vedtaksperioder =
                listOf(
                    vedtaksperiodeBeregningsgrunnlag(
                        fom = LocalDate.of(2025, 2, 1),
                        tom = LocalDate.of(2025, 2, 2),
                    ),
                )
            val perioder = vedtaksperioder.splittTilLøpendeMåneder()
            assertThat(perioder).isEmpty()
        }
    }

    @Nested
    inner class FlereVedtaksperioderSammeMåned {
        @Test
        fun `skal håndtere en vedtaksperiode som løper innenfor en løpende måned`() {
            val vedtaksperioder =
                listOf(
                    vedtaksperiodeBeregningsgrunnlag(
                        fom = LocalDate.of(2024, 1, 5),
                        tom = LocalDate.of(2024, 1, 15),
                    ),
                )
            val perioder = vedtaksperioder.splittTilLøpendeMåneder()
            assertThat(perioder).hasSize(1)
            with(perioder.single()) {
                assertThat(fom).isEqualTo(LocalDate.of(2024, 1, 5))
                assertThat(tom).isEqualTo(LocalDate.of(2024, 2, 4))
                assertThat(this.vedtaksperioder).containsExactly(
                    vedtaksperioder[0].forLøpendeMåned(),
                )
            }
        }

        @Test
        fun `skal håndtere to vedtaksperiode som løper i ulike løpende måneder`() {
            val vedtaksperioder =
                listOf(
                    vedtaksperiodeBeregningsgrunnlag(
                        fom = LocalDate.of(2024, 1, 5),
                        tom = LocalDate.of(2024, 1, 15),
                    ),
                    vedtaksperiodeBeregningsgrunnlag(
                        fom = LocalDate.of(2024, 2, 7),
                        tom = LocalDate.of(2024, 3, 2),
                    ),
                )
            val perioder = vedtaksperioder.splittTilLøpendeMåneder()
            assertThat(perioder).hasSize(2)
            with(perioder[0]) {
                assertThat(fom).isEqualTo(LocalDate.of(2024, 1, 5))
                assertThat(tom).isEqualTo(LocalDate.of(2024, 2, 4))
                assertThat(this.vedtaksperioder).containsExactly(
                    vedtaksperioder[0].forLøpendeMåned(),
                )
            }
            with(perioder[1]) {
                assertThat(fom).isEqualTo(LocalDate.of(2024, 2, 7))
                assertThat(tom).isEqualTo(LocalDate.of(2024, 3, 6))
                assertThat(this.vedtaksperioder).containsExactly(
                    vedtaksperioder[1].forLøpendeMåned(),
                )
            }
        }

        @Test
        fun `skal gruppere alle perioder som gjelder januar i januar`() {
            val vedtaksperioder =
                listOf(
                    vedtaksperiodeBeregningsgrunnlag(
                        fom = førsteJan2024,
                        tom = LocalDate.of(2024, 1, 5),
                    ),
                    vedtaksperiodeBeregningsgrunnlag(
                        fom = LocalDate.of(2024, 1, 7),
                        tom = LocalDate.of(2024, 1, 7),
                    ),
                )
            val perioder = vedtaksperioder.splittTilLøpendeMåneder()
            assertThat(perioder).hasSize(1)
            with(perioder.single()) {
                assertThat(fom).isEqualTo(førsteJan2024)
                assertThat(tom).isEqualTo(sisteJan2024)
                assertThat(this.vedtaksperioder).containsExactlyElementsOf(
                    vedtaksperioder.forLøpendeMåned(),
                )
            }
        }

        @Test
        fun `skal gruppere alle perioder som gjelder fra med 5 januar til 4 februar`() {
            val vedtaksperioder =
                listOf(
                    vedtaksperiodeBeregningsgrunnlag(
                        fom = LocalDate.of(2024, 1, 5),
                        tom = LocalDate.of(2024, 1, 5),
                    ),
                    vedtaksperiodeBeregningsgrunnlag(
                        fom = LocalDate.of(2024, 2, 1),
                        tom = LocalDate.of(2024, 2, 4),
                    ),
                )
            val perioder = vedtaksperioder.splittTilLøpendeMåneder()
            assertThat(perioder).hasSize(1)
            with(perioder.single()) {
                assertThat(fom).isEqualTo(LocalDate.of(2024, 1, 5))
                assertThat(tom).isEqualTo(LocalDate.of(2024, 2, 4))
                assertThat(this.vedtaksperioder).containsExactlyElementsOf(
                    vedtaksperioder.forLøpendeMåned(),
                )
            }
        }

        @Test
        fun `skal gruppere alle perioder som gjelder fra med 5 januar til 4 februar der periode 2 er siste dagen i forrige periode`() {
            val vedtaksperioder =
                listOf(
                    vedtaksperiodeBeregningsgrunnlag(
                        fom = LocalDate.of(2024, 1, 5),
                        tom = LocalDate.of(2024, 1, 5),
                    ),
                    vedtaksperiodeBeregningsgrunnlag(
                        fom = LocalDate.of(2024, 2, 4),
                        tom = LocalDate.of(2024, 2, 4),
                    ),
                )
            val perioder = vedtaksperioder.splittTilLøpendeMåneder()
            assertThat(perioder).hasSize(1)
            with(perioder.single()) {
                assertThat(fom).isEqualTo(LocalDate.of(2024, 1, 5))
                assertThat(tom).isEqualTo(LocalDate.of(2024, 2, 4))
                assertThat(this.vedtaksperioder).containsExactlyElementsOf(
                    vedtaksperioder.forLøpendeMåned(),
                )
            }
        }

        @Test
        fun `skal splitte periode 2 som løper over 2 vedtaksperioder`() {
            val vedtaksperioder =
                listOf(
                    vedtaksperiodeBeregningsgrunnlag(
                        fom = LocalDate.of(2024, 1, 5),
                        tom = LocalDate.of(2024, 1, 5),
                    ),
                    vedtaksperiodeBeregningsgrunnlag(
                        fom = LocalDate.of(2024, 2, 1),
                        tom = LocalDate.of(2024, 2, 28),
                    ),
                )
            val perioder = vedtaksperioder.splittTilLøpendeMåneder()
            assertThat(perioder).hasSize(2)
            with(perioder[0]) {
                assertThat(fom).isEqualTo(LocalDate.of(2024, 1, 5))
                assertThat(tom).isEqualTo(LocalDate.of(2024, 2, 4))
                assertThat(this.utbetalingsdato).isEqualTo(LocalDate.of(2024, 1, 5))
                assertThat(this.vedtaksperioder).containsExactly(
                    vedtaksperioder[0].forLøpendeMåned(),
                    VedtaksperiodeInnenforLøpendeMåned(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 4)),
                )
            }
            with(perioder[1]) {
                assertThat(fom).isEqualTo(LocalDate.of(2024, 2, 5))
                assertThat(tom).isEqualTo(LocalDate.of(2024, 3, 4))
                // Utbetalingsdato for periode 2 blir i neste måned fordi det blir en ny vedtaksperiode
                assertThat(this.utbetalingsdato).isEqualTo(LocalDate.of(2024, 2, 5))
                assertThat(this.vedtaksperioder).containsExactly(
                    VedtaksperiodeInnenforLøpendeMåned(LocalDate.of(2024, 2, 5), LocalDate.of(2024, 2, 28)),
                )
            }
        }
    }

    private fun VedtaksperiodeBeregningsgrunnlagLæremidler.forLøpendeMåned() = VedtaksperiodeInnenforLøpendeMåned(fom = fom, tom = tom)

    private fun List<VedtaksperiodeBeregningsgrunnlagLæremidler>.forLøpendeMåned() = this.map { it.forLøpendeMåned() }
}
