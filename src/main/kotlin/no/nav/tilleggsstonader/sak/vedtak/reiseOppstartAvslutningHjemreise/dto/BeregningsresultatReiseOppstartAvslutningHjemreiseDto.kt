package no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.dto

import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.domain.BeregningReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import java.math.BigDecimal
import java.time.LocalDate

data class BeregningsresultatReiseOppstartAvslutningHjemreiseDto(
    val offentligTransport: List<BeregningsresultatOffentligTransportDto>?,
    val privatBil: List<BeregningsresultatPrivatBilDto>?,
    val beregningsplan: Beregningsplan,
)

data class BeregningsresultatOffentligTransportDto(
    val reiseId: ReiseId,
    val adresse: String?,
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: BigDecimal,
    val aktivitetId: VilkårperiodeGlobalId,
)

data class BeregningsresultatPrivatBilDto(
    val reiseId: ReiseId,
    val adresse: String?,
    val fom: LocalDate,
    val tom: LocalDate,
    val sats: BigDecimal,
    val totaltReiseavstand: BigDecimal,
    val beløp: BigDecimal,
    val aktivitetId: VilkårperiodeGlobalId,
)

fun BeregningReiseOppstartAvslutningHjemreise.tilDto(beregningsplan: Beregningsplan) =
    BeregningsresultatReiseOppstartAvslutningHjemreiseDto(
        offentligTransport =
            offentligTransport
                .takeIf { it.isNotEmpty() }
                ?.map { it.tilDto() },
        privatBil =
            privatBil
                .takeIf { it.isNotEmpty() }
                ?.map { it.tilDto() },
        beregningsplan = beregningsplan,
    )

fun BeregningsresultatOffentligTransport.tilDto() =
    BeregningsresultatOffentligTransportDto(
        reiseId = reiseId,
        adresse = grunnlag.adresse,
        fom = grunnlag.fom,
        tom = grunnlag.tom,
        beløp = beløp,
        aktivitetId = aktivitetId,
    )

fun BeregningsresultatPrivatBil.tilDto() =
    BeregningsresultatPrivatBilDto(
        reiseId = reiseId,
        adresse = grunnlag.adresse,
        fom = grunnlag.fom,
        tom = grunnlag.tom,
        sats = grunnlag.sats,
        totaltReiseavstand = grunnlag.totaltReiseavstand,
        beløp = beløp,
        aktivitetId = aktivitetId,
    )
