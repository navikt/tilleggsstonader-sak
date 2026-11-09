package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise

import no.nav.tilleggsstonader.libs.feil.ApiFeil
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.util.lagreReiseOppstartAvslutningHjemreiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.TypeReiseformål
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ReiseOppstartAvslutningHjemreiseVilkårServiceTest {
    @Test
    fun `skal sette tom lik fom for oppstart når tom ikke er satt`() {
        val fom = 10 januar 2026

        val domain =
            lagreReiseOppstartAvslutningHjemreiseDto(
                fom = fom,
                tom = null,
                typeReiseformål = TypeReiseformål.OPPSTART,
            ).tilDomene()

        assertThat(domain.fom).isEqualTo(fom)
        assertThat(domain.tom).isEqualTo(fom)
    }

    @Test
    fun `skal feile om tom ikke er lik fom for avslutning`() {
        assertThatThrownBy {
            lagreReiseOppstartAvslutningHjemreiseDto(
                fom = 10 januar 2026,
                tom = 11 januar 2026,
                typeReiseformål = TypeReiseformål.AVSLUTNING,
            ).tilDomene()
        }.isInstanceOf(ApiFeil::class.java)
            .hasMessageContaining("Tom kan ikke settes til noe annet enn lik fom")
    }

    @Test
    fun `skal feile når tom mangler for hjemreise`() {
        assertThatThrownBy {
            lagreReiseOppstartAvslutningHjemreiseDto(
                fom = 10 januar 2026,
                tom = null,
                typeReiseformål = TypeReiseformål.HJEMREISE,
            ).tilDomene()
        }.isInstanceOf(ApiFeil::class.java)
            .hasMessageContaining("tom må settes for hjemreise")
    }
}
