package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.Vurdering
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class DelvilkårVilkårperiodeTest {

    @Nested
    inner class VurderingDelvilkår {

        @ParameterizedTest
        @EnumSource(value = SvarJaNei::class)
        fun `kan ikke ha svar når resultat=IKKE_AKTUELT`(svar: SvarJaNei) {
            assertThatThrownBy {
                Vurdering(svar, ResultatDelvilkårperiode.IKKE_AKTUELT)
            }.hasMessageContaining("Ugyldig resultat=IKKE_AKTUELT når svar=$svar")
        }
    }
}
