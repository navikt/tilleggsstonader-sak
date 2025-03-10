package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.AldersvilkårVurdering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class VurderingAldersvilkårTest {
    val fødselsdatoBruker = osloDateNow().minusYears(25)

    fun vurderingAldersvilkår(svar: SvarJaNei) =
        VurderingAldersVilkår(
            svar = svar,
            AldersvilkårVurdering.VurderingFaktaEtterlevelseAldersvilkår(
                fødselsdatoBruker,
            ),
        )

    @Test
    fun `hvis svar=JA så skal resultat=OPPFYLT`() {
        val vurdering = vurderingAldersvilkår(SvarJaNei.JA)

        assertThat(vurdering.resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
        assertThat(vurdering.svar).isEqualTo(SvarJaNei.JA)
        assertThat(
            vurdering.vurderingFaktaEtterlevelse,
        ).isEqualTo(AldersvilkårVurdering.VurderingFaktaEtterlevelseAldersvilkår(fødselsdatoBruker))
    }

    @Test
    fun `hvis svar=NEI så skal resultatet=IKKE_OPPFYLT`() {
        val vurdering = vurderingAldersvilkår(SvarJaNei.NEI)

        assertThat(vurdering.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_OPPFYLT)
        assertThat(vurdering.svar).isEqualTo(SvarJaNei.NEI)
        assertThat(
            vurdering.vurderingFaktaEtterlevelse,
        ).isEqualTo(AldersvilkårVurdering.VurderingFaktaEtterlevelseAldersvilkår(fødselsdatoBruker))
    }

    @Test
    fun `hvis svar=JA_IMPLISITT så skal det kastes feil`() {
        val feil = assertThrows<IllegalStateException> { vurderingAldersvilkår(SvarJaNei.JA_IMPLISITT) }
        assertThat(feil.message).isEqualTo("JA_IMPLISITT er ugyldig for VurderingAldersVilkår")
    }

    @Test
    fun `hvis svar=GAMMEL_MANGLER_DATA så skal resultatet=OPPFYLT`() {
        val vurdering = vurderingAldersvilkår(SvarJaNei.GAMMEL_MANGLER_DATA)

        assertThat(vurdering.resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
        assertThat(vurdering.svar).isEqualTo(SvarJaNei.GAMMEL_MANGLER_DATA)
        assertThat(
            vurdering.vurderingFaktaEtterlevelse,
        ).isEqualTo(null)
    }
}
