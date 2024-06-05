package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag

import no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import java.time.LocalDate
import java.time.LocalDateTime

data class VilkårperioderGrunnlagDto(
    val aktivitet: GrunnlagAktivitetDto,
    val ytelse: GrunnlagYtelseDto?,
    val hentetInformasjon: HentetInformasjonDto?,
)

data class GrunnlagAktivitetDto(
    val aktiviteter: List<AktivitetArenaDto>,
)

data class GrunnlagYtelseDto(
    val perioder: List<PeriodeGrunnlagYtelseDto>,
)

data class PeriodeGrunnlagYtelseDto(
    val type: TypeYtelsePeriode,
    val fom: LocalDate,
    val tom: LocalDate?,
)

data class HentetInformasjonDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val tidspunktHentet: LocalDateTime,
)

fun VilkårperioderGrunnlag.tilDto() =
    VilkårperioderGrunnlagDto(
        aktivitet = this.aktivitet.tilDto(),
        ytelse = this.ytelse.tilDto(),
        hentetInformasjon = this.hentetInformasjon.tilDto(),
    )

fun GrunnlagAktivitet.tilDto() =
    GrunnlagAktivitetDto(
        aktiviteter = this.aktiviteter,
    )

fun GrunnlagYtelse.tilDto() =
    GrunnlagYtelseDto(
        perioder = this.perioder.map { it.tilDto() },
    )

fun PeriodeGrunnlagYtelse.tilDto() =
    PeriodeGrunnlagYtelseDto(
        type = this.type,
        fom = this.fom,
        tom = this.tom,
    )

fun HentetInformasjon.tilDto() =
    HentetInformasjonDto(
        fom = this.fom,
        tom = this.tom,
        tidspunktHentet = this.tidspunktHentet,
    )
