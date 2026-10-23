package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain

import no.nav.tilleggsstonader.libs.feil.ApiFeil
import no.nav.tilleggsstonader.libs.feil.Feil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.NødvendigUtgiftDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.FaktaReiseOppstartAvslutningHjemreiseOffentligTransportDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.FaktaReiseOppstartAvslutningHjemreisePrivatBilDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class FaktaReiseOppstartAvslutningHjemreiseTest {
    @Test
    fun `skal mappe nødvendig utgift for offentlig transport`() {
        val fakta =
            FaktaReiseOppstartAvslutningHjemreiseOffentligTransportDto(
                nødvendigUtgift = NødvendigUtgiftDto(erUtgiftenNødvendig = SvarJaNei.JA, begrunnelse = null),
                utgifterOffentligTransport = 40.toBigDecimal(),
            ).mapTilFakta()

        assertThat(fakta.nødvendigUtgift.erUtgiftenNødvendig).isEqualTo(SvarJaNei.JA)
    }

    @Test
    fun `skal mappe nødvendig utgift for privat bil`() {
        val fakta =
            FaktaReiseOppstartAvslutningHjemreisePrivatBilDto(
                nødvendigUtgift = NødvendigUtgiftDto(erUtgiftenNødvendig = SvarJaNei.NEI, begrunnelse = "Ikke nødvendig"),
                reiseavstand = 40.toBigDecimal(),
                begrunnelse = null,
            ).mapTilFakta()

        assertThat(fakta.nødvendigUtgift.erUtgiftenNødvendig).isEqualTo(SvarJaNei.NEI)
    }

    @Test
    fun `skal kreve begrunnelse når nødvendig utgift er NEI`() {
        val feil =
            assertThrows<ApiFeil> {
                FaktaReiseOppstartAvslutningHjemreiseOffentligTransportDto(
                    nødvendigUtgift = NødvendigUtgiftDto(erUtgiftenNødvendig = SvarJaNei.NEI, begrunnelse = " "),
                    utgifterOffentligTransport = 40.toBigDecimal(),
                ).mapTilFakta()
            }

        assertThat(feil.message).isEqualTo("Begrunnelse må fylles ut når utgift ikke vurderes som nødvendig")
    }

    @Test
    fun `skal avvise GAMMEL_MANGLER_DATA i nødvendig utgift`() {
        val feil =
            assertThrows<Feil> {
                FaktaReiseOppstartAvslutningHjemreisePrivatBilDto(
                    nødvendigUtgift = NødvendigUtgiftDto(erUtgiftenNødvendig = SvarJaNei.GAMMEL_MANGLER_DATA, begrunnelse = null),
                    reiseavstand = BigDecimal.TEN,
                ).mapTilFakta()
            }

        assertThat(feil.message).isEqualTo("erUtgiftenNødvendig må være JA eller NEI")
    }
}
