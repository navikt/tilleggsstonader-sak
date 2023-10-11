package no.nav.tilleggsstonader.sak.vilkår.dto

import java.time.LocalDate

data class VilkårsvurderingDto(
    val vilkårsett: List<VilkårDto>,
    val grunnlag: VilkårGrunnlagDto,
)

data class VilkårGrunnlagDto(
    val barn: List<GrunnlagBarn>,
)

data class GrunnlagBarn(
    val ident: String,
    val registergrunnlag: RegistergrunnlagBarn,
    val søknadgrunnlag: SøknadsgrunnlagBarn,
)

data class RegistergrunnlagBarn(
    val navn: String,
    val dødsdato: LocalDate?,
)

data class SøknadsgrunnlagBarn(
    val søkt: Boolean, // TODO erstatt med annen info
)
