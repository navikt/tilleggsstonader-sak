package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.libs.feil.ApiFeil
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class VilkårAktivitetValideringTest {
    private val vilkårsperiode = Datoperiode(1 februar 2026, 28 februar 2026)

    @Test
    fun `oppfylt aktivitet som dekker hele perioden er gyldig`() {
        val aktivitet = aktivitet(fom = 1 februar 2026, tom = 31 mars 2026, resultat = ResultatVilkårperiode.OPPFYLT)

        assertThatCode { VilkårAktivitetValidering.validerAktivitetOppfyltHeleVilkåret(aktivitet, vilkårsperiode) }
            .doesNotThrowAnyException()
    }

    @Test
    fun `manglende aktivitet avvises`() {
        assertThatThrownBy { VilkårAktivitetValidering.validerAktivitetOppfyltHeleVilkåret(null, vilkårsperiode) }
            .isInstanceOf(ApiFeil::class.java)
            .hasMessageContaining("Aktiviteten finnes ikke")
    }

    @Test
    fun `aktivitet som ikke er oppfylt avvises`() {
        val aktivitet = aktivitet(fom = 1 februar 2026, tom = 28 februar 2026, resultat = ResultatVilkårperiode.IKKE_OPPFYLT)

        assertThatThrownBy { VilkårAktivitetValidering.validerAktivitetOppfyltHeleVilkåret(aktivitet, vilkårsperiode) }
            .isInstanceOf(ApiFeil::class.java)
            .hasMessageContaining("Aktiviteten er ikke oppfylt")
    }

    @Test
    fun `aktivitet som ikke dekker hele perioden avvises`() {
        val aktivitet = aktivitet(fom = 1 februar 2026, tom = 15 februar 2026, resultat = ResultatVilkårperiode.OPPFYLT)

        assertThatThrownBy { VilkårAktivitetValidering.validerAktivitetOppfyltHeleVilkåret(aktivitet, vilkårsperiode) }
            .isInstanceOf(ApiFeil::class.java)
            .hasMessageContaining("Aktiviteten er ikke oppfylt hele vilkårperioden")
    }
}
