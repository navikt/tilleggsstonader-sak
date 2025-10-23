package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain

import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FaktaDagligReiseTest {
    @Nested
    inner class OffentligTransport {
        @Test
        fun `skal kaste feil hvis negative utgifter`() {
            val feil =
                assertThrows<ApiFeil> {
                    FaktaOffentligTransport(
                        reisedagerPerUke = 4,
                        prisEnkelbillett = -44,
                        prisSyvdagersbillett = 200,
                        prisTrettidagersbillett = 780,
                    )
                }
            assertThat(feil.message).isEqualTo("Billettprisen må være større enn 0")
        }

        @Test
        fun `skal kaste feil hvis negative reisedager`() {
            val feil =
                assertThrows<ApiFeil> {
                    FaktaOffentligTransport(
                        reisedagerPerUke = -4,
                        prisEnkelbillett = 44,
                        prisSyvdagersbillett = 200,
                        prisTrettidagersbillett = 780,
                    )
                }
            assertThat(feil.message).isEqualTo("Reisedager per uke må være 0 eller mer")
        }

        @Test
        fun `skal kaste feil hvis reisedager er større enn 5`() {
            val feil =
                assertThrows<ApiFeil> {
                    FaktaOffentligTransport(
                        reisedagerPerUke = 6,
                        prisEnkelbillett = 44,
                        prisSyvdagersbillett = 200,
                        prisTrettidagersbillett = 780,
                    )
                }
            assertThat(feil.message).isEqualTo("Reisedager per uke kan ikke være mer enn 5")
        }

        @Test
        fun `skal kaste feil hvis ingen billettpriser er satt`() {
            val feil =
                assertThrows<ApiFeil> {
                    FaktaOffentligTransport(
                        reisedagerPerUke = 4,
                        prisEnkelbillett = null,
                        prisSyvdagersbillett = null,
                        prisTrettidagersbillett = null,
                    )
                }
            assertThat(feil.message).isEqualTo("Minst en billettpris må være satt")
        }
    }

    @Nested
    inner class PrivatBil {
        @Test
        fun `skal kaste feil hvis negative utgifter`() {
            val feil =
                assertThrows<ApiFeil> {
                    FaktaPrivatBil(
                        reisedagerPerUke = 4,
                        reiseavstandEnVei = 10,
                        prisBompengerPerDag = -10,
                        prisFergekostandPerDag = 0,
                    )
                }
            assertThat(feil.message).isEqualTo("Bompenge- og fergeprisen må være større enn 0")
        }

        @Test
        fun `skal kaste feil hvis negativ reiseavstand`() {
            val feil =
                assertThrows<ApiFeil> {
                    FaktaPrivatBil(
                        reisedagerPerUke = 4,
                        reiseavstandEnVei = -10,
                        prisBompengerPerDag = 0,
                        prisFergekostandPerDag = 0,
                    )
                }
            assertThat(feil.message).isEqualTo("Reiseavstanden må være større enn 0")
        }

        @Test
        fun `skal kaste feil hvis negativ reisedager`() {
            val feil =
                assertThrows<ApiFeil> {
                    FaktaPrivatBil(
                        reisedagerPerUke = -4,
                        reiseavstandEnVei = 10,
                        prisBompengerPerDag = 0,
                        prisFergekostandPerDag = 0,
                    )
                }
            assertThat(feil.message).isEqualTo("Reisedager per uke må være 0 eller mer")
        }

        @Test
        fun `skal kaste feil hvis reisedager er større enn 7`() {
            val feil =
                assertThrows<ApiFeil> {
                    FaktaPrivatBil(
                        reisedagerPerUke = 8,
                        reiseavstandEnVei = 10,
                        prisBompengerPerDag = 0,
                        prisFergekostandPerDag = 0,
                    )
                }
            assertThat(feil.message).isEqualTo("Reisedager per uke kan ikke være mer enn 7")
        }
    }
}
