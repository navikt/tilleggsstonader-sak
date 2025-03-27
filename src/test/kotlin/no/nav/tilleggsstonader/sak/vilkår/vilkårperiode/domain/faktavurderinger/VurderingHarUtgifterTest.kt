package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurderingHarUtgifter
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class VurderingHarUtgifterTest {
    @Test
    fun `hvis svar=NEI så skal resultatet=IKKE_OPPFYLT`() {
        val vurdering = vurderingHarUtgifter(SvarJaNei.NEI)

        assertThat(vurdering.svar).isEqualTo(SvarJaNei.NEI)
        assertThat(vurdering.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_OPPFYLT)
    }

    @Test
    fun `hvis svar=JA så skal resultat=OPPFYLT`() {
        val vurdering = vurderingHarUtgifter(SvarJaNei.JA)

        assertThat(vurdering.svar).isEqualTo(SvarJaNei.JA)
        assertThat(vurdering.resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
    }

    @Test
    fun `hvis svar=null så skal resultatet=IKKE_VURDERT`() {
        val vurdering = vurderingHarUtgifter(null)

        assertThat(vurdering.resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_VURDERT)
        assertThat(vurdering.svar).isNull()
    }

    @Test
    fun `svar=JA_IMPLISITT er ikke gyldig`() {
        assertThatThrownBy {
            vurderingHarUtgifter(SvarJaNei.JA_IMPLISITT)
        }.hasMessageContaining("ugyldig")
    }

    @Test
    fun `svar=NEI_IMPLISITT er ikke gyldig`() {
        assertThatThrownBy {
            vurderingHarUtgifter(SvarJaNei.NEI_IMPLISITT)
        }.hasMessageContaining("ugyldig")
    }
}
