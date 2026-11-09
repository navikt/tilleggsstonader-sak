package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise

import no.nav.tilleggsstonader.libs.feil.ApiFeil
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.LagreVilkårMidlertidigBosted
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.TypeReiseformål
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.FaktaReiseOppstartAvslutningHjemreiseUbestemtDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.LagreVilkårMidlertidigBostedDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.LagreVilkårReiseOppstartAvslutningHjemreiseDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate

class LagreVilkårROAHDtoTest {
    @Test
    fun `reise får vilkårstype ut fra reiseformål`() {
        assertThat(reise(TypeReiseformål.OPPSTART).tilDomene().vilkårType).isEqualTo(VilkårType.ROAH_OPPSTART_AVSLUTNING)
        assertThat(reise(TypeReiseformål.AVSLUTNING).tilDomene().vilkårType).isEqualTo(VilkårType.ROAH_OPPSTART_AVSLUTNING)
        assertThat(reise(TypeReiseformål.HJEMREISE).tilDomene().vilkårType).isEqualTo(VilkårType.ROAH_HJEMREISE)
    }

    @Test
    fun `reise krever tom for hjemreise, og tom lik fom ellers`() {
        assertThatThrownBy { reise(TypeReiseformål.HJEMREISE, tom = null).tilDomene() }
            .isInstanceOf(ApiFeil::class.java)
            .hasMessageContaining("tom må settes for hjemreise")
        assertThatThrownBy { reise(TypeReiseformål.OPPSTART, tom = 1 mars 2026).tilDomene() }
            .isInstanceOf(ApiFeil::class.java)
            .hasMessageContaining("Tom kan ikke settes")
        assertThat(reise(TypeReiseformål.OPPSTART, tom = null).tilDomene().tom).isEqualTo(1 januar 2026)
    }

    @Test
    fun `paraplyvilkår får vilkårstype midlertidig bosted`() {
        val domene = paraply().tilDomene()

        assertThat(domene).isInstanceOf(LagreVilkårMidlertidigBosted::class.java)
        assertThat(domene.vilkårType).isEqualTo(VilkårType.ROAH_MIDLERTIDIG_BOSTED_PERIODE)
    }

    private fun reise(
        reiseformål: TypeReiseformål,
        tom: LocalDate? = if (reiseformål == TypeReiseformål.HJEMREISE) 1 mars 2026 else null,
    ) = LagreVilkårReiseOppstartAvslutningHjemreiseDto(
        fom = 1 januar 2026,
        tom = tom,
        reiseId = ReiseId.random(),
        aktivitetId = VilkårperiodeGlobalId.random(),
        typeReiseformål = reiseformål,
        svar = emptyMap(),
        fakta = FaktaReiseOppstartAvslutningHjemreiseUbestemtDto,
    )

    private fun paraply() =
        LagreVilkårMidlertidigBostedDto(
            fom = 1 januar 2026,
            tom = 1 mars 2026,
            aktivitetId = VilkårperiodeGlobalId.random(),
            svar = emptyMap(),
            fraAdresse = "A",
            tilAdresse = "B",
        )
}
