package no.nav.tilleggsstonader.sak.vilkår.dto

data class VilkårsvurderingDto(
    val vilkårsett: List<VilkårDto>,
    val grunnlag: VilkårGrunnlagDto,
)

data class VilkårGrunnlagDto(
    val fellesgrunnlag: Fellesgrunnlag,
)

data class Fellesgrunnlag(
    val navn: String,
)
