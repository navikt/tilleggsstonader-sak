package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.grunnlagsdataDomain
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.AldersvilkårVurdering.vurderAldersvilkår
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.dummyVilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AldersvilkårVurderingTest {
    @Test
    fun `vurdering skal være riktig`() {
        val målgruppe = dummyVilkårperiodeMålgruppe()
        val grunnlagsdata = grunnlagsdataDomain()

        vurderAldersvilkår(målgruppe, grunnlagsdata).also {
            assertEquals(SvarJaNei.JA, it)
        }
    }
}
