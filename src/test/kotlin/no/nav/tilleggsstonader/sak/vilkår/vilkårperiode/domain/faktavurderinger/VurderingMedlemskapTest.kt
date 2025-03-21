package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingMedlemskap
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class VurderingMedlemskapTest {
    @Test
    fun `hvis svar=NEI så skal resultatet=IKKE_OPPFYLT`() {
        val vurdering = vurderingMedlemskap(SvarJaNei.NEI)

        assertThat(vurdering.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_OPPFYLT)
        assertThat(vurdering.svar).isEqualTo(SvarJaNei.NEI)
    }

    @Test
    fun `hvis svar=JA så skal resultat=OPPFYLT`() {
        val vurdering = vurderingMedlemskap(SvarJaNei.JA)

        assertThat(vurdering.resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
        assertThat(vurdering.svar).isEqualTo(SvarJaNei.JA)
    }

    @Test
    fun `hvis svar=JA_IMPLISITT så skal resultat=OPPFYLT`() {
        val vurdering = vurderingMedlemskap(SvarJaNei.JA_IMPLISITT)

        assertThat(vurdering.resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
        assertThat(vurdering.svar).isEqualTo(SvarJaNei.JA_IMPLISITT)
    }

    @Test
    fun `hvis svar=null så skal resultatet=IKKE_VURDERT`() {
        val vurdering = vurderingMedlemskap(null)

        assertThat(vurdering.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_VURDERT)
        assertThat(vurdering.svar).isNull()
    }

    @Test
    fun `svar=NEI_IMPLISITT er ikke gyldig`() {
        assertThatThrownBy {
            vurderingMedlemskap(SvarJaNei.NEI_IMPLISITT)
        }.hasMessageContaining("ugyldig")
    }
}
