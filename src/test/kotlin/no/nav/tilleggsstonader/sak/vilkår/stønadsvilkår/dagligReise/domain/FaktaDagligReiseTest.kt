package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

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

        @Test
        fun `skal kaste feil dersom perioden er over 30-dager, antall reisedager er mer eller lik tre og man mangler 30-dagersbillett`() {
            val feil =
                assertThrows<ApiFeil> {
                    FaktaOffentligTransport(
                        reisedagerPerUke = 3,
                        prisEnkelbillett = 44,
                        prisSyvdagersbillett = null,
                        prisTrettidagersbillett = null,
                        periode = Datoperiode(fom = LocalDate.now(), tom = LocalDate.now().plusDays(31)),
                    )
                }
            assertThat(feil.message).isEqualTo("Du er nødt til å fylle ut pris for 30-dagersbillett")
        }

        @Test
        fun `skal kaste feil dersom perioden fulle 30-dager perioder og man mangler trettidagersbillett`() {
            val feil =
                assertThrows<ApiFeil> {
                    FaktaOffentligTransport(
                        reisedagerPerUke = 3,
                        prisEnkelbillett = 44,
                        prisSyvdagersbillett = null,
                        prisTrettidagersbillett = null,
                        periode = Datoperiode(fom = LocalDate.now(), tom = LocalDate.now().plusDays(29)),
                    )
                }
            assertThat(feil.message).isEqualTo("Fyll ut pris for 30-dagersbillett")
        }

        @Test
        fun `skal kaste feil dersom perioden er mindre enn 30-dager, reisedager mindre enn tre og man mangler enkeltbillett`() {
            val feil =
                assertThrows<ApiFeil> {
                    FaktaOffentligTransport(
                        reisedagerPerUke = 2,
                        prisEnkelbillett = null,
                        prisSyvdagersbillett = null,
                        prisTrettidagersbillett = 750,
                        periode = Datoperiode(fom = LocalDate.now(), tom = LocalDate.now().plusDays(20)),
                    )
                }
            assertThat(feil.message).isEqualTo("Du er nødt til å fylle ut pris for enkeltbillett")
        }

        @Test
        fun `skal kaste feil hvis perioden ikke går opp i hele 30-dagersperioder`() {
            val feil =
                assertThrows<ApiFeil> {
                    FaktaOffentligTransport(
                        reisedagerPerUke = 3,
                        prisEnkelbillett = null,
                        prisSyvdagersbillett = null,
                        prisTrettidagersbillett = 750,
                        periode = Datoperiode(fom = LocalDate.now(), tom = LocalDate.now().plusDays(31)),
                    )
                }
            assertThat(feil.message).isEqualTo("Fyll ut pris for enkeltbillett")
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
