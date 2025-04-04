package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.UtgiftBeregning
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.vedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningUtil.brukPerioderFraOgMedRevurderFra
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningUtil.tilÅrMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.vedtaksperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.Temporal

class TilsynBeregningUtilTest {
    val behandlingId = BehandlingId.random()

    val januar = YearMonth.of(2024, 1)
    val februar = YearMonth.of(2024, 2)
    val mars = YearMonth.of(2024, 3)

    @Nested
    inner class SplitVedtaksperioder {
        @Nested
        inner class EnVedtaksperiode {
            @Test
            fun `vedtaksperiode innenfor en måned skal kun gi en måned ut`() {
                val vedtaksperiode =
                    vedtaksperiodeBeregning(
                        fom = januar.atDay(3),
                        tom = januar.atDay(21),
                    )
                val vedtaksperioder =
                    listOf(
                        vedtaksperiode,
                    ).sorted()

                val resultat = vedtaksperioder.tilÅrMåned()

                assertThat(resultat.values).hasSize(1)
                assertThat(resultat[januar]).isEqualTo(vedtaksperioder)
            }

            @Test
            fun `skal splitte en vedtaksperiode over flere måneder`() {
                val vedtaksperiode =
                    vedtaksperiodeBeregning(
                        fom = LocalDate.of(2024, 1, 1),
                        tom = LocalDate.of(2024, 3, 31),
                    )

                val vedtaksperioder = listOf(vedtaksperiode).sorted()

                val resultat = vedtaksperioder.tilÅrMåned()

                assertThat(resultat.values).hasSize(3)
                resultat.values.forEach { assertThat(it).hasSize(1) }

                resultat[januar]?.get(0)?.harVerdier(fom = vedtaksperiode.fom, januar.atEndOfMonth())
                resultat[februar]?.get(0)?.harVerdier(fom = februar.atDay(1), februar.atEndOfMonth())
                resultat[mars]?.get(0)?.harVerdier(fom = mars.atDay(1), mars.atEndOfMonth())
            }
        }

        @Nested
        inner class FlereVedtaksperioder {
            @Test
            fun `flere vedtaksperioder innenfor en måned`() {
                val vedtaksperiode1 =
                    vedtaksperiodeBeregning(
                        fom = LocalDate.of(2024, 1, 1),
                        tom = LocalDate.of(2024, 1, 10),
                    )

                val vedtaksperiode2 =
                    vedtaksperiodeBeregning(
                        fom = LocalDate.of(2024, 1, 20),
                        tom = LocalDate.of(2024, 1, 31),
                    )

                val vedtaksperioder =
                    listOf(
                        vedtaksperiode1,
                        vedtaksperiode2,
                    ).sorted()

                val resultat = vedtaksperioder.tilÅrMåned()

                assertThat(resultat.values).hasSize(1)

                val resultatJanuar = resultat[januar]!!
                assertThat(resultatJanuar).hasSize(2)

                resultatJanuar[0].harVerdier(fom = vedtaksperiode1.fom, vedtaksperiode1.tom)
                resultatJanuar[1].harVerdier(fom = vedtaksperiode2.fom, vedtaksperiode2.tom)
            }

            @Test
            fun `skal splitte flere vedtaksperiode over flere måneder`() {
                val vedtaksperiode1 =
                    vedtaksperiodeBeregning(
                        fom = januar.atDay(1),
                        tom = februar.atDay(10),
                    )
                val vedtaksperiode2 =
                    vedtaksperiodeBeregning(
                        fom = februar.atDay(11),
                        tom = mars.atDay(20),
                    )

                val vedtaksperioder =
                    listOf(
                        vedtaksperiode1,
                        vedtaksperiode2,
                    ).sorted()

                val resultat = vedtaksperioder.tilÅrMåned()

                assertThat(resultat.values).hasSize(3)

                resultat[januar]?.get(0)?.harVerdier(fom = vedtaksperiode1.fom, tom = januar.atEndOfMonth())
                resultat[februar]?.get(0)?.harVerdier(fom = februar.atDay(1), tom = vedtaksperiode1.tom)
                resultat[februar]?.get(1)?.harVerdier(fom = vedtaksperiode2.fom, tom = februar.atEndOfMonth())
                resultat[mars]?.get(0)?.harVerdier(fom = mars.atDay(1), tom = vedtaksperiode2.tom)
            }
        }
    }

    // TODO: Test for utgifter

    // TODO: Test for aktiviteter

    @Nested
    inner class ValideringAvVedtaksperioderOgUtgifter {
        val vedtaksperioder =
            listOf(
                vedtaksperiodeBeregningsgrunnlag(
                    fom = LocalDate.of(2025, 1, 1),
                    tom = LocalDate.of(2025, 2, 28),
                    målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                    aktivitet = AktivitetType.TILTAK,
                ),
            )

        val barn1 = BarnId.random()
        val barn2 = BarnId.random()

        val utgiftJanuar =
            UtgiftBeregning(
                fom = YearMonth.of(2025, 1),
                tom = YearMonth.of(2025, 1),
                utgift = 1000,
            )
        val utgiftFebruar =
            UtgiftBeregning(
                fom = YearMonth.of(2025, 2),
                tom = YearMonth.of(2025, 2),
                utgift = 1000,
            )
        val utgiftJanuarTilFebruar =
            UtgiftBeregning(
                fom = YearMonth.of(2025, 1),
                tom = YearMonth.of(2025, 2),
                utgift = 1000,
            )

        val utgifter =
            mapOf(
                barn1 to
                    listOf(
                        utgiftJanuar,
                        utgiftFebruar,
                    ),
                barn2 to
                    listOf(
                        utgiftJanuarTilFebruar,
                    ),
            )

        @Nested
        inner class BrukPerioderFraOgMedRevurderFra {
            @Test
            fun `skal returnere orginal liste uten revurder fra`() {
                assertThat(vedtaksperioder.brukPerioderFraOgMedRevurderFra(null)).isEqualTo(vedtaksperioder)
            }

            @Test
            fun `skal returnere perioder etter revurder fra`() {
                val revurderFra = LocalDate.of(2025, 2, 1)

                val forventedeVedtaksperioder =
                    listOf(
                        vedtaksperiodeBeregningsgrunnlag(
                            fom = LocalDate.of(2025, 2, 1),
                            tom = LocalDate.of(2025, 2, 28),
                            målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                            aktivitet = AktivitetType.TILTAK,
                        ),
                    )

                assertThat(
                    vedtaksperioder
                        .brukPerioderFraOgMedRevurderFra(revurderFra),
                ).isEqualTo(forventedeVedtaksperioder)
            }
        }
    }

    private fun <T> Periode<T>.harVerdier(
        fom: T,
        tom: T,
    ) where T : Comparable<T>, T : Temporal {
        assertThat(this.fom).isEqualTo(fom)
        assertThat(this.tom).isEqualTo(tom)
    }
}
