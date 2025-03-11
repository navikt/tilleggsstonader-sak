package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingDekketAvAnnetRegelverk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class VurderingDekketAvAnnetRegelverkTest {
    @Test
    fun `hvis svar=NEI så skal resultatet=OPPFYLT`() {
        val vurdering = vurderingDekketAvAnnetRegelverk(SvarJaNei.NEI)

        assertThat(vurdering.resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
        assertThat(vurdering.svar).isEqualTo(SvarJaNei.NEI)
    }

    @Test
    fun `hvis svar=JA så skal resultat=IKKE_OPPFYLT`() {
        val vurdering = vurderingDekketAvAnnetRegelverk(SvarJaNei.JA)

        assertThat(vurdering.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_OPPFYLT)
        assertThat(vurdering.svar).isEqualTo(SvarJaNei.JA)
    }

    @Test
    fun `hvis svar=null så skal resultatet=IKKE_VURDERT`() {
        val vurdering = vurderingDekketAvAnnetRegelverk(null)

        assertThat(vurdering.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_VURDERT)
        assertThat(vurdering.svar).isNull()
    }

    @Test
    fun `svar=JA_IMPLISITT er ikke gyldig`() {
        assertThatThrownBy {
            vurderingDekketAvAnnetRegelverk(SvarJaNei.JA_IMPLISITT)
        }.hasMessageContaining("ugyldig")
    }

    @Test
    fun `svar=NEI_IMPLISITT er ikke gyldig`() {
        assertThatThrownBy {
            vurderingDekketAvAnnetRegelverk(SvarJaNei.NEI_IMPLISITT)
        }.hasMessageContaining("ugyldig")
    }
}
