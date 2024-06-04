package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag

import no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto
import java.time.LocalDateTime

data class VilkårperioderGrunnlagDto(
    val aktivitet: GrunnlagAktivitetDto,
)

data class GrunnlagAktivitetDto(
    val aktiviteter: List<AktivitetArenaDto>,
    val tidspunktHentet: LocalDateTime,
)

fun VilkårperioderGrunnlag.tilDto() =
    VilkårperioderGrunnlagDto(
        aktivitet = this.aktivitet.tilDto(),
    )

fun GrunnlagAktivitet.tilDto() =
    GrunnlagAktivitetDto(
        aktiviteter = this.aktiviteter,
        tidspunktHentet = this.tidspunktHentet,
    )
