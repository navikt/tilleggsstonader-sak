package no.nav.tilleggsstonader.sak.vilkår

import io.mockk.mockk
import no.nav.tilleggsstonader.sak.vilkår.dto.Fellesgrunnlag
import no.nav.tilleggsstonader.sak.vilkår.dto.VilkårGrunnlagDto

object VilkårTestUtil {

    fun mockVilkårGrunnlagDto(
        fellesgrunnlag: Fellesgrunnlag = mockk(),
    ) =
        VilkårGrunnlagDto(
            fellesgrunnlag = fellesgrunnlag,
        )
}
