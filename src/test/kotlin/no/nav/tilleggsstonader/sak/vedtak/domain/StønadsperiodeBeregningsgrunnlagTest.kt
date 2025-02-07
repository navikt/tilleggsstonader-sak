package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningV1.TilsynBeregningUtil.splitFraRevurderFra
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class StønadsperiodeBeregningsgrunnlagTest {
    @Nested
    inner class SplitFraRevurderFra {
        val stønadsperioder =
            listOf(
                StønadsperiodeBeregningsgrunnlag(
                    fom = LocalDate.of(2025, 1, 1),
                    tom = LocalDate.of(2025, 2, 28),
                    målgruppe = MålgruppeType.AAP,
                    aktivitet = AktivitetType.TILTAK,
                ),
            )

        @Test
        fun `skal splitte på revurder fra`() {
            val revurderFra = LocalDate.of(2025, 2, 1)

            val forventetStønadsperioder =
                listOf(
                    StønadsperiodeBeregningsgrunnlag(
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 31),
                        målgruppe = MålgruppeType.AAP,
                        aktivitet = AktivitetType.TILTAK,
                    ),
                    StønadsperiodeBeregningsgrunnlag(
                        fom = LocalDate.of(2025, 2, 1),
                        tom = LocalDate.of(2025, 2, 28),
                        målgruppe = MålgruppeType.AAP,
                        aktivitet = AktivitetType.TILTAK,
                    ),
                )

            assertThat(stønadsperioder.splitFraRevurderFra(revurderFra)).isEqualTo(forventetStønadsperioder)
        }

        @Test
        fun `skal ikke splitte når ingen revurder fra`() {
            assertThat(stønadsperioder.splitFraRevurderFra(null)).isEqualTo(stønadsperioder)
        }
    }
}
