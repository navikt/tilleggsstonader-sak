package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class VilkårTypeTest {

    private val vilkårForBarnetilsyn = listOf(
        VilkårType.MÅLGRUPPE,
        VilkårType.AKTIVITET,
        VilkårType.PASS_BARN,
    )

    @Test
    internal fun `skal hente ut vilkår for barnetilsyn`() {
        val filtrerteVilkårstyper = VilkårType.hentVilkårForStønad(Stønadstype.BARNETILSYN)
        assertThat(filtrerteVilkårstyper).containsExactlyInAnyOrderElementsOf(vilkårForBarnetilsyn)
    }
}
