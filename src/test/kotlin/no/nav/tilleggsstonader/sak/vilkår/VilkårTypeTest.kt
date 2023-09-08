package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.sak.fagsak.Stønadstype
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class VilkårTypeTest {

    private val vilkårForBarnetilsyn = listOf(
        VilkårType.EKSEMPEL,
        VilkårType.EKSEMPEL2,
    )

    @Test
    internal fun `skal hente ut vilkår for barnetilsyn`() {
        val filtrerteVilkårstyper = VilkårType.hentVilkårForStønad(Stønadstype.BARNETILSYN)
        assertThat(filtrerteVilkårstyper).containsExactlyInAnyOrderElementsOf(vilkårForBarnetilsyn)
    }
}
