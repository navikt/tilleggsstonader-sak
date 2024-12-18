package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtil.tilÅrMåned
import no.nav.tilleggsstonader.sak.vedtak.domain.tilSortertStønadsperiodeBeregningsgrunnlag
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
                val stønadsperiode = stønadsperiode(
                    behandlingId = behandlingId,
                    fom = januar.atDay(3),
                    tom = januar.atDay(21),
                )
                val stønadsperioder = listOf(
                    stønadsperiode,
                ).tilSortertStønadsperiodeBeregningsgrunnlag()

                val resultat = stønadsperioder.tilÅrMåned()

                assertThat(resultat.values).hasSize(1)
                assertThat(resultat[januar]).isEqualTo(stønadsperioder)
            }

            @Test
            fun `skal splitte en stønadsperiode over flere måneder`() {
                val stønadsperiode = stønadsperiode(
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
                val stønadsperiode1 = stønadsperiode(
                    behandlingId = behandlingId,
                    fom = LocalDate.of(2024, 1, 1),
                    tom = LocalDate.of(2024, 1, 10),
                )

                val stønadsperiode2 = stønadsperiode(
                    behandlingId = behandlingId,
                    fom = LocalDate.of(2024, 1, 20),
                    tom = LocalDate.of(2024, 1, 31),
                )

                val stønadsperioder = listOf(
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
                val stønadsperiode1 = stønadsperiode(
                    behandlingId = behandlingId,
                    fom = januar.atDay(1),
                    tom = februar.atDay(10),
                )
                val stønadsperiode2 = stønadsperiode(
                    behandlingId = behandlingId,
                    fom = februar.atDay(11),
                    tom = mars.atDay(20),
                )

                val stønadsperioder = listOf(
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

    private fun <T> Periode<T>.harVerdier(fom: T, tom: T) where T : Comparable<T>, T : Temporal {
        assertThat(this.fom).isEqualTo(fom)
        assertThat(this.tom).isEqualTo(tom)
    }
}
