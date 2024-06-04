package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag

import no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto
import java.time.LocalDate
import java.time.LocalDateTime

data class VilkårperioderGrunnlagDto(
    val aktivitet: GrunnlagAktivitetDto,
)

data class GrunnlagAktivitetDto(
    val aktiviteter: List<AktivitetArenaDto>,
    val hentetInformasjon: HentetInformasjonDto,
)

fun VilkårperioderGrunnlag.tilDto() =
    VilkårperioderGrunnlagDto(
        aktivitet = this.aktivitet.tilDto(),
    )

fun GrunnlagAktivitet.tilDto() =
    GrunnlagAktivitetDto(
        aktiviteter = this.aktiviteter,
        hentetInformasjon = this.hentetInformasjon.tilDto(),
    )

data class HentetInformasjonDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val tidspunktHentet: LocalDateTime,
)

fun HentetInformasjon.tilDto() = HentetInformasjonDto(
    fom = this.fom,
    tom = this.tom,
    tidspunktHentet = this.tidspunktHentet,
)