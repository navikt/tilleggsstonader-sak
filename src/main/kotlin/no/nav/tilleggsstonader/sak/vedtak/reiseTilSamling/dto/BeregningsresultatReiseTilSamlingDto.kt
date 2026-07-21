package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto

import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.beregning.BeregningReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatOffentligTransportForSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatPrivatBilForSamling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import java.math.BigDecimal
import java.time.LocalDate

data class BeregningsresultatReiseTilSamlingDto(
    val offentligTransport: BeregningsresultatOffentligTransportDto?,
    val privatBil: BeregningsresultatPrivatBilDto?,
)

data class BeregningsresultatOffentligTransportDto(
    val samlinger: List<BeregningsresultatForSamlingDto>,
)

data class BeregningsresultatForSamlingDto(
    val reiseId: ReiseId,
    val adresse: String?,
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: BigDecimal,
)

data class BeregningsresultatPrivatBilDto(
    val samlinger: List<BeregningsresultatPrivatBilForSamlingDto>,
)

data class BeregningsresultatPrivatBilForSamlingDto(
    val reiseId: ReiseId,
    val adresse: String?,
    val fom: LocalDate,
    val tom: LocalDate,
    val sats: BigDecimal,
    val totaltReiseavstand: BigDecimal,
    val beløp: BigDecimal,
)

fun BeregningReiseTilSamling.tilDto(): BeregningsresultatReiseTilSamlingDto {
    val offentligTransport =
        samlinger
            .filterIsInstance<BeregningsresultatOffentligTransport>()
            .singleOrNull()
    val privatBil =
        samlinger
            .filterIsInstance<BeregningsresultatPrivatBil>()
            .singleOrNull()

    return BeregningsresultatReiseTilSamlingDto(
        offentligTransport = offentligTransport?.tilDto(),
        privatBil = privatBil?.tilDto(),
    )
}

fun BeregningsresultatOffentligTransport.tilDto() =
    BeregningsresultatOffentligTransportDto(
        samlinger = reiser.map { it.tilDto() },
    )

fun BeregningsresultatOffentligTransportForSamling.tilDto() =
    BeregningsresultatForSamlingDto(
        reiseId = reiseId,
        adresse = grunnlag.adresse,
        fom = grunnlag.fom,
        tom = grunnlag.tom,
        beløp = beløp,
    )

fun BeregningsresultatPrivatBil.tilDto() =
    BeregningsresultatPrivatBilDto(
        samlinger = samlinger.map { it.tilDto() },
    )

fun BeregningsresultatPrivatBilForSamling.tilDto() =
    BeregningsresultatPrivatBilForSamlingDto(
        reiseId = reiseId,
        adresse = grunnlag.adresse,
        fom = grunnlag.fom,
        tom = grunnlag.tom,
        sats = grunnlag.sats,
        totaltReiseavstand = grunnlag.totaltReiseavstand,
        beløp = beløp,
    )
