package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningV1

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.TilsynBarnBeregningValideringUtilFelles.erOverlappMellomPerioderOgUtgifter
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningV1.TilsynBeregningUtil.brukPerioderFraOgMedRevurderFra
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningV1.TilsynBeregningUtil.brukPerioderFraOgMedRevurderFraMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningV1.TilsynBeregningUtil.tilÅrMåned
import no.nav.tilleggsstonader.sak.vedtak.domain.StønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.domain.tilSortertStønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
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
    inner class SplitStønadsperioder {
        @Nested
        inner class EnStønadsperiode {
            @Test
            fun `stønadsperiode innenfor en måned skal kun gi en måned ut`() {
                val stønadsperiode =
                    stønadsperiode(
                        behandlingId = behandlingId,
                        fom = januar.atDay(3),
                        tom = januar.atDay(21),
                    )
                val stønadsperioder =
                    listOf(
                        stønadsperiode,
                    ).tilSortertStønadsperiodeBeregningsgrunnlag()

                val resultat = stønadsperioder.tilÅrMåned()

                assertThat(resultat.values).hasSize(1)
                assertThat(resultat[januar]).isEqualTo(stønadsperioder)
            }

            @Test
            fun `skal splitte en stønadsperiode over flere måneder`() {
                val stønadsperiode =
                    stønadsperiode(
                        behandlingId = behandlingId,
                        fom = LocalDate.of(2024, 1, 1),
                        tom = LocalDate.of(2024, 3, 31),
                    )

                val stønadsperioder = listOf(stønadsperiode).tilSortertStønadsperiodeBeregningsgrunnlag()

                val resultat = stønadsperioder.tilÅrMåned()

                assertThat(resultat.values).hasSize(3)
                resultat.values.forEach { assertThat(it).hasSize(1) }

                resultat[januar]?.get(0)?.harVerdier(fom = stønadsperiode.fom, januar.atEndOfMonth())
                resultat[februar]?.get(0)?.harVerdier(fom = februar.atDay(1), februar.atEndOfMonth())
                resultat[mars]?.get(0)?.harVerdier(fom = mars.atDay(1), mars.atEndOfMonth())
            }
        }

        @Nested
        inner class FlereStønadsperioder {
            @Test
            fun `flere stønadsperioder innenfor en måned`() {
                val stønadsperiode1 =
                    stønadsperiode(
                        behandlingId = behandlingId,
                        fom = LocalDate.of(2024, 1, 1),
                        tom = LocalDate.of(2024, 1, 10),
                    )

                val stønadsperiode2 =
                    stønadsperiode(
                        behandlingId = behandlingId,
                        fom = LocalDate.of(2024, 1, 20),
                        tom = LocalDate.of(2024, 1, 31),
                    )

                val stønadsperioder =
                    listOf(
                        stønadsperiode1,
                        stønadsperiode2,
                    ).tilSortertStønadsperiodeBeregningsgrunnlag()

                val resultat = stønadsperioder.tilÅrMåned()

                assertThat(resultat.values).hasSize(1)

                val resultatJanuar = resultat[januar]!!
                assertThat(resultatJanuar).hasSize(2)

                resultatJanuar[0].harVerdier(fom = stønadsperiode1.fom, stønadsperiode1.tom)
                resultatJanuar[1].harVerdier(fom = stønadsperiode2.fom, stønadsperiode2.tom)
            }

            @Test
            fun `skal splitte flere stønadsperiode over flere måneder`() {
                val stønadsperiode1 =
                    stønadsperiode(
                        behandlingId = behandlingId,
                        fom = januar.atDay(1),
                        tom = februar.atDay(10),
                    )
                val stønadsperiode2 =
                    stønadsperiode(
                        behandlingId = behandlingId,
                        fom = februar.atDay(11),
                        tom = mars.atDay(20),
                    )

                val stønadsperioder =
                    listOf(
                        stønadsperiode1,
                        stønadsperiode2,
                    ).tilSortertStønadsperiodeBeregningsgrunnlag()

                val resultat = stønadsperioder.tilÅrMåned()

                assertThat(resultat.values).hasSize(3)

                resultat[januar]?.get(0)?.harVerdier(fom = stønadsperiode1.fom, tom = januar.atEndOfMonth())
                resultat[februar]?.get(0)?.harVerdier(fom = februar.atDay(1), tom = stønadsperiode1.tom)
                resultat[februar]?.get(1)?.harVerdier(fom = stønadsperiode2.fom, tom = februar.atEndOfMonth())
                resultat[mars]?.get(0)?.harVerdier(fom = mars.atDay(1), tom = stønadsperiode2.tom)
            }
        }
    }

    // TODO: Test for utgifter

    // TODO: Test for aktiviteter

    @Nested
    inner class ValideringAvStønadsperioderOgUtgifter {
        val stønadsperioder =
            listOf(
                StønadsperiodeBeregningsgrunnlag(
                    fom = LocalDate.of(2025, 1, 1),
                    tom = LocalDate.of(2025, 2, 28),
                    målgruppe = MålgruppeType.AAP,
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
        val utgiftMars =
            UtgiftBeregning(
                fom = YearMonth.of(2025, 3),
                tom = YearMonth.of(2025, 3),
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
        inner class OverlappMellomStønadsperioderOgUtgifter {
            @Test
            fun `skal retunere true når flere barn overlapper`() {
                assertThat(erOverlappMellomPerioderOgUtgifter(stønadsperioder, utgifter)).isTrue
            }

            @Test
            fun `skal retunere true når kun ett barn overlapper`() {
                val utgifter =
                    mapOf(
                        barn1 to
                            listOf(
                                utgiftJanuar,
                                utgiftFebruar,
                            ),
                        barn2 to
                            listOf(
                                utgiftMars,
                            ),
                    )
                assertThat(erOverlappMellomPerioderOgUtgifter(stønadsperioder, utgifter)).isTrue
            }

            @Test
            fun `skal retunere false når ikke overlapp`() {
                val stønadsperioder =
                    listOf(
                        StønadsperiodeBeregningsgrunnlag(
                            fom = LocalDate.of(2025, 3, 1),
                            tom = LocalDate.of(2025, 3, 31),
                            målgruppe = MålgruppeType.AAP,
                            aktivitet = AktivitetType.TILTAK,
                        ),
                    )

                assertThat(erOverlappMellomPerioderOgUtgifter(stønadsperioder, utgifter)).isFalse
            }
        }

        @Nested
        inner class BrukPerioderFraOgMedRevurderFra {
            @Test
            fun `skal returnere orginal liste uten revurder fra`() {
                assertThat(stønadsperioder.brukPerioderFraOgMedRevurderFra(null)).isEqualTo(stønadsperioder)
            }

            @Test
            fun `skal returnere perioder etter revurder fra`() {
                val revurderFra = LocalDate.of(2025, 2, 1)

                val forventedeStønadsperioder =
                    listOf(
                        StønadsperiodeBeregningsgrunnlag(
                            fom = LocalDate.of(2025, 2, 1),
                            tom = LocalDate.of(2025, 2, 28),
                            målgruppe = MålgruppeType.AAP,
                            aktivitet = AktivitetType.TILTAK,
                        ),
                    )

                assertThat(
                    stønadsperioder
                        .brukPerioderFraOgMedRevurderFra(revurderFra),
                ).isEqualTo(forventedeStønadsperioder)
            }
        }

        @Nested
        inner class BrukPerioderFraOgMedRevurderFraMåned {
            @Test
            fun `skal returnere orginal map uten revurder fra`() {
                assertThat(utgifter.brukPerioderFraOgMedRevurderFraMåned(null)).isEqualTo(utgifter)
            }

            @Test
            fun `skal returnere perioder etter revurder fra`() {
                val revurderFra = LocalDate.of(2025, 2, 1)

                val forventedeUtgifter =
                    mapOf(
                        barn1 to
                            listOf(
                                utgiftFebruar,
                            ),
                        barn2 to
                            listOf(
                                utgiftFebruar,
                            ),
                    )
                assertThat(
                    utgifter
                        .brukPerioderFraOgMedRevurderFraMåned(revurderFra),
                ).isEqualTo(forventedeUtgifter)
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
