package no.nav.tilleggsstonader.sak.vilkår.dto

data class VilkårDto(
    val vurderinger: List<VilkårsvurderingDto>,
    val grunnlag: VilkårGrunnlagDto,
)

data class VilkårGrunnlagDto(
    val fellesgrunnlag: Fellesgrunnlag,
)

data class Fellesgrunnlag(
    val navn: String,
)
