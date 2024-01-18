package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class DelvilkårVilkårperiodeTest {

    @Nested
    inner class VurderingDelvilkår {

        @Test
        fun `kan ikke ha begrunnelse sammen når svar=JA_IMPLISITT`() {
            assertThatThrownBy {
                DelvilkårVilkårperiode.Vurdering(SvarJaNei.JA_IMPLISITT, "", ResultatDelvilkårperiode.OPPFYLT)
            }.hasMessageContaining("Kan ikke ha begrunnelse når svar=JA_IMPLISITT")
        }

        @Test
        fun `kan ikke ha begrunnelse sammen med resultat=IKKE_AKTUELT`() {
            assertThatThrownBy {
                DelvilkårVilkårperiode.Vurdering(null, "", ResultatDelvilkårperiode.IKKE_AKTUELT)
            }.hasMessageContaining("Ugyldig resultat=IKKE_AKTUELT når svar=null begrunnelseErNull=false")
        }

        @ParameterizedTest
        @EnumSource(value = SvarJaNei::class)
        fun `kan ikke ha svar når resultat=IKKE_AKTUELT`(svar: SvarJaNei) {
            assertThatThrownBy {
                DelvilkårVilkårperiode.Vurdering(svar, null, ResultatDelvilkårperiode.IKKE_AKTUELT)
            }.hasMessageContaining("Ugyldig resultat=IKKE_AKTUELT når svar=$svar begrunnelseErNull=true")
        }
    }
}
