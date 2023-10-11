package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.sak.vilkår.dto.GrunnlagAktivitet
import no.nav.tilleggsstonader.sak.vilkår.dto.GrunnlagHovedytelse
import no.nav.tilleggsstonader.sak.vilkår.dto.VilkårGrunnlagDto

object VilkårGrunnlagUtil {
    fun mockVilkårGrunnlagDto() =
        VilkårGrunnlagDto(
            hovedytelse = GrunnlagHovedytelse(
                søknadsgrunnlag = null,
            ),
            aktivitet = GrunnlagAktivitet(
                søknadsgrunnlag = null,
            ),
            barn = emptyList(),
        )
}
