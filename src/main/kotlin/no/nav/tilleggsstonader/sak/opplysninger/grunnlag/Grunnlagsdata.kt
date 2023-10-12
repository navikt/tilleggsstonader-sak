package no.nav.tilleggsstonader.sak.opplysninger.grunnlag

import java.time.LocalDateTime

data class GrunnlagsdataMedMetadata(
    val grunnlagsdata: Grunnlagsdata,
    val opprettetTidspunkt: LocalDateTime,
)

data class Grunnlagsdata(
    val barn: List<GrunnlagsdataBarn>,
)
