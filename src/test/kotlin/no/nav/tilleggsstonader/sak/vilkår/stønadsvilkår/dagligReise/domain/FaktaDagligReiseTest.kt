package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.util.faktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaDelperiodePrivatBil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class FaktaDagligReiseTest {
    @Nested
    inner class OffentligTransport {
        @Test
        fun `skal kaste feil hvis negative utgifter`() {
            val feil = assertThrows<ApiFeil> { faktaOffentligTransport(prisEnkelbillett = -44) }
            assertThat(feil.message).isEqualTo("Billettprisen må være større enn 0")
        }

        @Test
        fun `skal kaste feil hvis negative reisedager`() {
            val feil = assertThrows<ApiFeil> { faktaOffentligTransport(reisedagerPerUke = -4) }
            assertThat(feil.message).isEqualTo("Reisedager per uke må være 0 eller mer")
        }

        @Test
        fun `skal kaste feil hvis reisedager er større enn 5`() {
            val feil = assertThrows<ApiFeil> { faktaOffentligTransport(reisedagerPerUke = 6) }
            assertThat(feil.message).isEqualTo("Reisedager per uke kan ikke være mer enn 5")
        }

        @Test
        fun `skal kaste feil hvis ingen billettpriser er satt`() {
            val feil =
                assertThrows<ApiFeil> {
                    faktaOffentligTransport(
                        prisEnkelbillett = null,
                        prisSyvdagersbillett = null,
                        prisTrettidagersbillett = null,
                    )
                }
            assertThat(feil.message).isEqualTo("Minst en billettpris må være satt")
        }

        @Test
        fun `skal kaste feil dersom perioden er mindre enn 30-dager, reisedager mindre enn tre og man mangler enkeltbillett`() {
            val feil =
                assertThrows<ApiFeil> {
                    faktaOffentligTransport(
                        reisedagerPerUke = 1,
                        prisEnkelbillett = null,
                        periode = Datoperiode(fom = LocalDate.now(), tom = LocalDate.now().plusDays(20)),
                    )
                }
            assertThat(feil.message).isEqualTo("Pris for enkeltbillett må fylles ut når det reises sjeldent eller over en kort periode")
        }

        @Test
        fun `skal kaste feil dersom perioden er mindre enn 30-dager og man mangler enkeltbillett`() {
            val feil =
                assertThrows<ApiFeil> {
                    faktaOffentligTransport(
                        prisEnkelbillett = null,
                        periode = Datoperiode(fom = LocalDate.now(), tom = LocalDate.now().plusDays(12)),
                    )
                }
            assertThat(feil.message).isEqualTo("Pris for enkeltbillett må fylles ut når det reises sjeldent eller over en kort periode")
        }

        @Test
        fun `skal kaste feil dersom perioden er mindre enn 30 dager, reisedager mer enn tre og man mangler 30-dagersbillett`() {
            val feil =
                assertThrows<ApiFeil> {
                    faktaOffentligTransport(
                        prisTrettidagersbillett = null,
                        periode = Datoperiode(fom = LocalDate.now(), tom = LocalDate.now().plusDays(28)),
                    )
                }
            assertThat(
                feil.message,
            ).isEqualTo("Pris for 30-dagersbillett må fylles ut da det lønner seg med 30-dagersbillett for denne perioden")
        }

        @Test
        fun `skal kaste feil dersom perioden er over 30-dager, antall reisedager er mer eller lik tre og man mangler 30-dagersbillett`() {
            val feil =
                assertThrows<ApiFeil> {
                    faktaOffentligTransport(
                        prisTrettidagersbillett = null,
                        periode = Datoperiode(fom = LocalDate.now(), tom = LocalDate.now().plusDays(31)),
                    )
                }
            assertThat(feil.message).isEqualTo("Pris for 30-dagersbillett må fylles ut når det reises regelmessig over lengre tid")
        }

        @Test
        fun `skal kaste feil dersom perioden er 30-dager og man mangler trettidagersbillett`() {
            val feil =
                assertThrows<ApiFeil> {
                    faktaOffentligTransport(
                        prisTrettidagersbillett = null,
                        periode = Datoperiode(fom = LocalDate.now(), tom = LocalDate.now().plusDays(29)),
                    )
                }
            assertThat(feil.message).isEqualTo("Pris for 30-dagersbillett må fylles ut når det reises regelmessig over lengre tid")
        }

        @Test
        fun `skal kaste feil hvis perioden går opp i hele 30-dagersperioder og man ikke har 30-dagersbillett`() {
            val feil =
                assertThrows<ApiFeil> {
                    faktaOffentligTransport(
                        prisTrettidagersbillett = null,
                        periode = Datoperiode(fom = LocalDate.now(), tom = LocalDate.now().plusDays(47)),
                    )
                }
            assertThat(
                feil.message,
            ).isEqualTo("Pris for 30-dagersbillett må fylles ut når det reises regelmessig over lengre tid")
        }

        @Test
        fun `skal kaste feil hvis perioden ikke går opp i hele 30-dagersperioder`() {
            val feil =
                assertThrows<ApiFeil> {
                    faktaOffentligTransport(
                        prisEnkelbillett = null,
                        periode = Datoperiode(fom = LocalDate.now(), tom = LocalDate.now().plusDays(47)),
                    )
                }
            assertThat(
                feil.message,
            ).isEqualTo("Pris for enkeltbillett må fylles ut siden reisen varer lenger enn 30 dager uten å være en eksakt 30-dagersperiode")
        }
    }

    @Nested
    inner class PrivatBil {
        @Test
        fun `skal kaste feil hvis negative bompenger`() {
            val feil =
                assertThrows<ApiFeil> {
                    FaktaPrivatBil(
                        reiseId = ReiseId.random(),
                        adresse = "Tiltaksveien 1",
                        faktaDelperioder =
                            listOf(
                                FaktaDelperiodePrivatBil(
                                    fom = LocalDate.now(),
                                    tom = LocalDate.now().plusDays(10),
                                    reisedagerPerUke = 4,
                                    bompengerPerDag = -10,
                                    fergekostnadPerDag = 0,
                                ),
                            ),
                        reiseavstandEnVei = BigDecimal(10),
                        aktivitetId = UUID.randomUUID(),
                    )
                }
            assertThat(feil.message).isEqualTo("Bompengeprisen må være større enn 0")
        }

        @Test
        fun `skal kaste feil hvis negative fergekostander`() {
            val feil =
                assertThrows<ApiFeil> {
                    FaktaPrivatBil(
                        reiseId = ReiseId.random(),
                        adresse = "Tiltaksveien 1",
                        faktaDelperioder =
                            listOf(
                                FaktaDelperiodePrivatBil(
                                    fom = LocalDate.now(),
                                    tom = LocalDate.now().plusDays(10),
                                    reisedagerPerUke = 4,
                                    bompengerPerDag = 0,
                                    fergekostnadPerDag = -10,
                                ),
                            ),
                        reiseavstandEnVei = BigDecimal(10),
                        aktivitetId = UUID.randomUUID(),
                    )
                }
            assertThat(feil.message).isEqualTo("Fergekostnaden må være større enn 0")
        }

        @Test
        fun `skal kaste feil hvis negativ reiseavstand`() {
            val feil =
                assertThrows<ApiFeil> {
                    FaktaPrivatBil(
                        reiseId = ReiseId.random(),
                        adresse = "Tiltaksveien 1",
                        faktaDelperioder =
                            listOf(
                                FaktaDelperiodePrivatBil(
                                    fom = LocalDate.now(),
                                    tom = LocalDate.now().plusDays(10),
                                    reisedagerPerUke = 4,
                                    bompengerPerDag = 0,
                                    fergekostnadPerDag = 0,
                                ),
                            ),
                        reiseavstandEnVei = BigDecimal("-10"),
                        aktivitetId = UUID.randomUUID(),
                    )
                }
            assertThat(feil.message).isEqualTo("Reiseavstanden må være større enn 0")
        }

        @Test
        fun `skal kaste feil hvis negativ reisedager`() {
            val feil =
                assertThrows<ApiFeil> {
                    FaktaPrivatBil(
                        reiseId = ReiseId.random(),
                        adresse = "Tiltaksveien 1",
                        reiseavstandEnVei = BigDecimal(10),
                        faktaDelperioder =
                            listOf(
                                FaktaDelperiodePrivatBil(
                                    fom = LocalDate.now(),
                                    tom = LocalDate.now().plusDays(10),
                                    reisedagerPerUke = -4,
                                    bompengerPerDag = 0,
                                    fergekostnadPerDag = 0,
                                ),
                            ),
                        aktivitetId = UUID.randomUUID(),
                    )
                }
            assertThat(feil.message).isEqualTo("Reisedager per uke må være større enn 0")
        }

        @Test
        fun `skal kaste feil hvis reisedager er større enn 7`() {
            val feil =
                assertThrows<ApiFeil> {
                    FaktaPrivatBil(
                        reiseId = ReiseId.random(),
                        adresse = "Tiltaksveien 1",
                        reiseavstandEnVei = BigDecimal(10),
                        faktaDelperioder =
                            listOf(
                                FaktaDelperiodePrivatBil(
                                    fom = LocalDate.now(),
                                    tom = LocalDate.now().plusDays(10),
                                    reisedagerPerUke = 8,
                                    bompengerPerDag = 0,
                                    fergekostnadPerDag = 0,
                                ),
                            ),
                        aktivitetId = UUID.randomUUID(),
                    )
                }
            assertThat(feil.message).isEqualTo("Reisedager per uke kan ikke være mer enn 7")
        }
    }
}
