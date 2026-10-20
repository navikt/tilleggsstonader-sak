package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.MålgruppeValidering.validerNyeMålgrupperOverlapperIkkeMedEksisterendeMålgrupper
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import org.assertj.core.api.Assertions.assertThatNoException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MålgruppeValideringTest {
    @Nested
    inner class ValiderNyeMålgrupperOverlapperIkkeMedEksisterendeMålgrupper {
        @Test
        fun `skal ikke kaste feil hvis det ikke er overlapp`() {
            val vilkårperioder =
                Vilkårperioder(
                    målgrupper =
                        listOf(
                            målgruppe(
                                fom = LocalDate.of(2025, 1, 1),
                                tom = LocalDate.of(2025, 1, 31),
                                status = Vilkårstatus.NY,
                            ),
                            målgruppe(
                                fom = LocalDate.of(2025, 3, 1),
                                tom = LocalDate.of(2025, 3, 31),
                                status = Vilkårstatus.UENDRET,
                            ),
                        ),
                    aktiviteter = emptyList(),
                )

            assertThatNoException().isThrownBy {
                validerNyeMålgrupperOverlapperIkkeMedEksisterendeMålgrupper(vilkårperioder)
            }
        }

        @Test
        fun `skal kaste feil hvis nye målgrupper overlapper med tidligere`() {
            val vilkårperioder =
                Vilkårperioder(
                    målgrupper =
                        listOf(
                            målgruppe(
                                fom = LocalDate.of(2025, 1, 1),
                                tom = LocalDate.of(2025, 3, 31),
                                status = Vilkårstatus.NY,
                            ),
                            målgruppe(
                                fom = LocalDate.of(2025, 3, 1),
                                tom = LocalDate.of(2025, 5, 31),
                                status = Vilkårstatus.NY,
                            ),
                            målgruppe(
                                fom = LocalDate.of(2025, 1, 1),
                                tom = LocalDate.of(2025, 1, 31),
                                status = Vilkårstatus.UENDRET,
                            ),
                            målgruppe(
                                fom = LocalDate.of(2025, 3, 1),
                                tom = LocalDate.of(2025, 3, 31),
                                status = Vilkårstatus.ENDRET,
                            ),
                        ),
                    aktiviteter = emptyList(),
                )
            assertThatThrownBy {
                validerNyeMålgrupperOverlapperIkkeMedEksisterendeMålgrupper(vilkårperioder)
            }.hasMessage(
                """
                Du kan ikke legge til nye målgrupper som overlapper med eksisterende målgrupper:

                Perioden AAP 01.01.2025–31.03.2025 overlapper med:
                  - 01.01.2025–31.01.2025
                  - 01.03.2025–31.03.2025
                Perioden AAP 01.03.2025–31.05.2025 overlapper med:
                  - 01.03.2025–31.03.2025
                
                """.trimIndent(),
            )
        }
    }
}
