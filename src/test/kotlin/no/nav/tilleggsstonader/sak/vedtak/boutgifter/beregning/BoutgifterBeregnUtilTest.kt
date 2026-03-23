package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.lagUtgiftBeregningBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.BoutgifterBeregnUtil.splittTilLøpendeMåneder
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.BoutgifterBeregnUtil.splittVedGrensenTilFaktiskeUtgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningTestUtil.vedtaksperiodeBeregning
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BoutgifterBeregnUtilTest {
    @Nested
    inner class SplittTilLøpendeMåneder {
        @Test
        fun `splitter vedtaksperiode på løpende måneder`() {
            val vedtaksperioder =
                listOf(
                    vedtaksperiodeBeregning(
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 3, 31),
                    ),
                )

            val perioder = vedtaksperioder.splittTilLøpendeMåneder()

            assertThat(perioder).hasSize(3)
            with(perioder[0]) {
                assertThat(fom).isEqualTo(LocalDate.of(2025, 1, 1))
                assertThat(tom).isEqualTo(LocalDate.of(2025, 1, 31))
                assertThat(this.vedtaksperioder).containsExactly(
                    vedtaksperiodeBeregning(fom = LocalDate.of(2025, 1, 1), tom = LocalDate.of(2025, 1, 31)).forLøpendeMåned(),
                )
            }
            with(perioder[1]) {
                assertThat(fom).isEqualTo(LocalDate.of(2025, 2, 1))
                assertThat(tom).isEqualTo(LocalDate.of(2025, 2, 28))
                assertThat(this.vedtaksperioder).containsExactly(
                    vedtaksperiodeBeregning(fom = LocalDate.of(2025, 2, 1), tom = LocalDate.of(2025, 2, 28)).forLøpendeMåned(),
                )
            }
            with(perioder[2]) {
                assertThat(fom).isEqualTo(LocalDate.of(2025, 3, 1))
                assertThat(tom).isEqualTo(LocalDate.of(2025, 3, 31))
                assertThat(this.vedtaksperioder).containsExactly(
                    vedtaksperiodeBeregning(fom = LocalDate.of(2025, 3, 1), tom = LocalDate.of(2025, 3, 31)).forLøpendeMåned(),
                )
            }
        }
    }

    @Nested
    inner class SplittVedGrensenTilFaktiskeUtgifter {
        @Test
        fun `uten faktiske utgifter returnerer ett segment lik input`() {
            val vedtaksperioder =
                listOf(
                    vedtaksperiodeBeregning(
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 3, 31),
                    ),
                )
            val utgifter =
                mapOf(
                    TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG to
                        listOf(
                            lagUtgiftBeregningBoutgifter(
                                fom = LocalDate.of(2025, 1, 1),
                                tom = LocalDate.of(2025, 3, 31),
                                skalFåDekketFaktiskeUtgifter = false,
                            ),
                        ),
                )

            val segmenter = vedtaksperioder.splittVedGrensenTilFaktiskeUtgifter(utgifter)

            assertThat(segmenter).hasSize(1)
            assertThat(segmenter.first().perioder).isEqualTo(vedtaksperioder)
        }

        @Test
        fun `ett kuttepunkt gir to segmenter splittet ved kuttdato`() {
            val vedtaksperioder =
                listOf(
                    vedtaksperiodeBeregning(
                        fom = LocalDate.of(2025, 9, 15),
                        tom = LocalDate.of(2026, 6, 2),
                    ),
                )
            val utgifter =
                mapOf(
                    TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG to
                        listOf(
                            lagUtgiftBeregningBoutgifter(
                                fom = LocalDate.of(2026, 3, 1),
                                tom = LocalDate.of(2026, 6, 2),
                                skalFåDekketFaktiskeUtgifter = true,
                            ),
                        ),
                )

            val segmenter = vedtaksperioder.splittVedGrensenTilFaktiskeUtgifter(utgifter)

            assertThat(segmenter).hasSize(2)
            val seg0 = segmenter.first()
            val seg1 = segmenter.last()
            assertThat(seg0).hasSize(1)
            assertThat(seg0.first().fom).isEqualTo(LocalDate.of(2025, 9, 15))
            assertThat(seg0.first().tom).isEqualTo(LocalDate.of(2026, 2, 28))
            assertThat(seg1).hasSize(1)
            assertThat(seg1.first().fom).isEqualTo(LocalDate.of(2026, 3, 1))
            assertThat(seg1.first().tom).isEqualTo(LocalDate.of(2026, 6, 2))
        }

        @Test
        fun `kuttdato lik vedtaksperiode fom gir ett segment`() {
            val vedtaksperioder =
                listOf(
                    vedtaksperiodeBeregning(
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 3, 31),
                    ),
                )
            val utgifter =
                mapOf(
                    TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG to
                        listOf(
                            lagUtgiftBeregningBoutgifter(
                                fom = LocalDate.of(2025, 1, 1),
                                tom = LocalDate.of(2025, 3, 31),
                                skalFåDekketFaktiskeUtgifter = true,
                            ),
                        ),
                )

            val segmenter = vedtaksperioder.splittVedGrensenTilFaktiskeUtgifter(utgifter)

            assertThat(segmenter).hasSize(1)
            val seg = segmenter.first()
            assertThat(seg).hasSize(1)
            assertThat(seg.first().fom).isEqualTo(LocalDate.of(2025, 1, 1))
            assertThat(seg.first().tom).isEqualTo(LocalDate.of(2025, 3, 31))
        }

        @Test
        fun `to kuttepunkter gir tre segmenter`() {
            val vedtaksperioder =
                listOf(
                    vedtaksperiodeBeregning(
                        fom = LocalDate.of(2025, 1, 15),
                        tom = LocalDate.of(2025, 6, 30),
                    ),
                )
            val utgifter =
                mapOf(
                    TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG to
                        listOf(
                            lagUtgiftBeregningBoutgifter(
                                fom = LocalDate.of(2025, 3, 1),
                                tom = LocalDate.of(2025, 4, 30),
                                skalFåDekketFaktiskeUtgifter = true,
                            ),
                            lagUtgiftBeregningBoutgifter(
                                fom = LocalDate.of(2025, 5, 1),
                                tom = LocalDate.of(2025, 6, 30),
                                skalFåDekketFaktiskeUtgifter = true,
                            ),
                        ),
                )

            val segmenter = vedtaksperioder.splittVedGrensenTilFaktiskeUtgifter(utgifter)

            assertThat(segmenter).hasSize(3)
            val seg0 = segmenter[0]
            val seg1 = segmenter[1]
            val seg2 = segmenter[2]
            assertThat(seg0.first().fom).isEqualTo(LocalDate.of(2025, 1, 15))
            assertThat(seg0.first().tom).isEqualTo(LocalDate.of(2025, 2, 28))
            assertThat(seg1.first().fom).isEqualTo(LocalDate.of(2025, 3, 1))
            assertThat(seg1.first().tom).isEqualTo(LocalDate.of(2025, 4, 30))
            assertThat(seg2.first().fom).isEqualTo(LocalDate.of(2025, 5, 1))
            assertThat(seg2.first().tom).isEqualTo(LocalDate.of(2025, 6, 30))
        }

        @Test
        fun `pipeline - to segmenter gir 10 loepende maaneder med riktige grenser`() {
            val vedtaksperioder =
                listOf(
                    vedtaksperiodeBeregning(
                        fom = LocalDate.of(2025, 9, 15),
                        tom = LocalDate.of(2026, 6, 2),
                    ),
                )
            val utgifter =
                mapOf(
                    TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG to
                        listOf(
                            lagUtgiftBeregningBoutgifter(
                                fom = LocalDate.of(2026, 3, 1),
                                tom = LocalDate.of(2026, 6, 2),
                                skalFåDekketFaktiskeUtgifter = true,
                            ),
                        ),
                )

            val segmenter = vedtaksperioder.splittVedGrensenTilFaktiskeUtgifter(utgifter)
            val perioder = segmenter.flatMap { segment -> segment.perioder.splittTilLøpendeMåneder() }

            assertThat(perioder).hasSize(10)

            // Siste periode i segment 1 starter 2026-02-15
            assertThat(perioder[5].fom).isEqualTo(LocalDate.of(2026, 2, 15))

            // Første periode i segment 2 starter fra kuttdatoen
            assertThat(perioder[6].fom).isEqualTo(LocalDate.of(2026, 3, 1))
            assertThat(perioder[6].tom).isEqualTo(LocalDate.of(2026, 3, 31))
        }
    }

    private fun VedtaksperiodeBeregning.forLøpendeMåned(
        fom: LocalDate = this.fom,
        tom: LocalDate = this.tom,
    ) = VedtaksperiodeInnenforLøpendeMåned(
        fom = fom,
        tom = tom,
        målgruppe = målgruppe,
        aktivitet = aktivitet,
    )
}
