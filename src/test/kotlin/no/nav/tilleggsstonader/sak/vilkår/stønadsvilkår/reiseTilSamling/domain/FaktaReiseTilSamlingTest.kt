package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain

import no.nav.tilleggsstonader.libs.feil.ApiFeil
import no.nav.tilleggsstonader.sak.util.dummyReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.FaktaReiseTilSamlingPrivatBilDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class FaktaReiseTilSamlingTest {
    @Nested
    inner class PrivatBil {
        private fun faktaPrivatBil(
            bompenger: BigDecimal? = null,
            fergekostnad: BigDecimal? = null,
            parkering: BigDecimal? = null,
            piggdekkavgift: BigDecimal? = null,
            begrunnelse: String? = "Spesifisert privatbilutgift",
        ) = FaktaPrivatBil(
            reiseId = dummyReiseId,
            adresse = "Tiltaksveien 1",
            reiseavstand = 40.toBigDecimal(),
            begrunnelse = begrunnelse,
            bompenger = bompenger,
            fergekostnad = fergekostnad,
            parkering = parkering,
            piggdekkavgift = piggdekkavgift,
        )

        @Test
        fun `skal godta bompenger akkurat på maksgrensen`() {
            faktaPrivatBil(bompenger = FaktaPrivatBil.MAKS_BOMPENGER)
        }

        @Test
        fun `skal kaste feil hvis bompenger er høyere enn maksgrensen`() {
            val feil =
                assertThrows<ApiFeil> {
                    faktaPrivatBil(bompenger = FaktaPrivatBil.MAKS_BOMPENGER.add(BigDecimal.ONE))
                }
            assertThat(feil.message)
                .isEqualTo("Skal du innvilge med bompenger høyere enn 500kr må du ta kontakt med Tilleggsstønader-teamet")
        }

        @Test
        fun `skal godta fergekostnad akkurat på maksgrensen`() {
            faktaPrivatBil(fergekostnad = FaktaPrivatBil.MAKS_FERGEKOSTNAD)
        }

        @Test
        fun `skal kaste feil hvis fergekostnad er høyere enn maksgrensen`() {
            val feil =
                assertThrows<ApiFeil> {
                    faktaPrivatBil(fergekostnad = FaktaPrivatBil.MAKS_FERGEKOSTNAD.add(BigDecimal.ONE))
                }
            assertThat(feil.message)
                .isEqualTo("Skal du innvilge med fergekostnad høyere enn 900kr må du ta kontakt med Tilleggsstønader-teamet")
        }

        @Test
        fun `skal godta parkering akkurat på maksgrensen`() {
            faktaPrivatBil(parkering = FaktaPrivatBil.MAKS_PARKERING)
        }

        @Test
        fun `skal kaste feil hvis parkering er høyere enn maksgrensen`() {
            val feil =
                assertThrows<ApiFeil> {
                    faktaPrivatBil(parkering = FaktaPrivatBil.MAKS_PARKERING.add(BigDecimal.ONE))
                }
            assertThat(feil.message)
                .isEqualTo("Skal du innvilge med parkering høyere enn 2000kr må du ta kontakt med Tilleggsstønader-teamet")
        }

        @Test
        fun `skal godta piggdekkavgift akkurat på maksgrensen`() {
            faktaPrivatBil(piggdekkavgift = FaktaPrivatBil.MAKS_PIGGDEKKAVGIFT)
        }

        @Test
        fun `skal kaste feil hvis piggdekkavgift er høyere enn maksgrensen`() {
            val feil =
                assertThrows<ApiFeil> {
                    faktaPrivatBil(piggdekkavgift = FaktaPrivatBil.MAKS_PIGGDEKKAVGIFT.add(BigDecimal.ONE))
                }
            assertThat(feil.message)
                .isEqualTo("Skal du innvilge med piggdekkavgift høyere enn 1400kr må du ta kontakt med Tilleggsstønader-teamet")
        }

        @Test
        fun `skal fortsatt kaste feil hvis utgift er negativ`() {
            val feil = assertThrows<ApiFeil> { faktaPrivatBil(bompenger = BigDecimal("-1")) }
            assertThat(feil.message).isEqualTo("Bompenger kan ikke være negativt")
        }

        @Test
        fun `skal godta null begrunnelse siden spesifikasjon av utgift er valgfritt for privatbil`() {
            assertDoesNotThrow { faktaPrivatBil(begrunnelse = null) }
        }

        @Test
        fun `skal godta blank begrunnelse siden spesifikasjon av utgift er valgfritt for privatbil`() {
            assertDoesNotThrow { faktaPrivatBil(begrunnelse = "   ") }
        }
    }

    @Nested
    inner class OffentligTransport {
        @Test
        fun `skal kaste feil hvis begrunnelse er blank`() {
            val feil =
                assertThrows<ApiFeil> {
                    FaktaOffentligTransport(
                        reiseId = dummyReiseId,
                        adresse = "Tiltaksveien 1",
                        utgifterOffentligTransport = 40.toBigDecimal(),
                        begrunnelse = " ",
                    )
                }
            assertThat(feil.message).isEqualTo("Spesifikasjon av utgift må fylles ut")
        }
    }

    @Nested
    inner class PrivatBilDto {
        private fun dto(begrunnelse: String?) =
            FaktaReiseTilSamlingPrivatBilDto(
                reiseavstand = 40.toBigDecimal(),
                begrunnelse = begrunnelse,
            )

        @Test
        fun `skal normalisere blank begrunnelse til null`() {
            val fakta = dto("   ").mapTilFakta(reiseId = dummyReiseId, adresse = "Tiltaksveien 1")
            assertThat(fakta.begrunnelse).isNull()
        }

        @Test
        fun `skal beholde null begrunnelse som null`() {
            val fakta = dto(null).mapTilFakta(reiseId = dummyReiseId, adresse = "Tiltaksveien 1")
            assertThat(fakta.begrunnelse).isNull()
        }
    }
}
