package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto

import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import java.math.BigDecimal
import java.time.LocalDate

data class BeregningsresultatReiseTilSamlingDto(
    val offentligTransport: List<BeregningsresultatOffentligTransportDto>?,
    val privatBil: List<BeregningsresultatPrivatBilDto>?,
)

data class BeregningsresultatOffentligTransportDto(
    val reiseId: ReiseId,
    val adresse: String?,
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: BigDecimal,
)

data class BeregningsresultatPrivatBilDto(
    val reiseId: ReiseId,
    val adresse: String?,
    val fom: LocalDate,
    val tom: LocalDate,
    val sats: BigDecimal,
    val totaltReiseavstand: BigDecimal,
    val beløp: BigDecimal,
)

fun BeregningReiseTilSamling.tilDto() =
    BeregningsresultatReiseTilSamlingDto(
        offentligTransport =
            offentligTransport
                .takeIf { it.isNotEmpty() }
                ?.map { it.tilDto() },
        privatBil =
            privatBil
                .takeIf { it.isNotEmpty() }
                ?.map { it.tilDto() },
    )

fun BeregningsresultatOffentligTransport.tilDto() =
    BeregningsresultatOffentligTransportDto(
        reiseId = reiseId,
        adresse = grunnlag.adresse,
        fom = grunnlag.fom,
        tom = grunnlag.tom,
        beløp = beløp,
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
    )
