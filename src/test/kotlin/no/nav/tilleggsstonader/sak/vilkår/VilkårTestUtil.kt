package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.sak.vilkår.dto.VilkårGrunnlagDto

object VilkårTestUtil {

    fun mockVilkårGrunnlagDto() =
        VilkårGrunnlagDto(
            barn = emptyList(),
        )
}
