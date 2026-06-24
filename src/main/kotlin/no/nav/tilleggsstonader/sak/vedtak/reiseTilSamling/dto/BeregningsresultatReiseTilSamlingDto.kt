package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto

import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.beregning.BeregningReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatForSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.VilkårReiseTilSamling
import java.time.LocalDate

data class BeregningsresultatReiseTilSamlingDto(
    val offentligTransport: BeregningsresultatOffentligTransportDto?,
)

data class BeregningsresultatOffentligTransportDto(
    val samling: List<BeregningsresultatForSamlingDto>,
    val beløp: Int,
)

data class BeregningsresultatForSamlingDto(
    val reiseId: ReiseId,
    val adresse: String?,
    val fom: LocalDate,
    val tom: LocalDate,
    val utgifterOffentligTransport: Int?,
)

fun BeregningReiseTilSamling.tilDto(vilkår: List<VilkårReiseTilSamling>): BeregningsresultatReiseTilSamlingDto =
    BeregningsresultatReiseTilSamlingDto(
        offentligTransport = beregningsresultatOffentligTransport.tilDto(),
    )

fun BeregningsresultatOffentligTransport.tilDto() =
    BeregningsresultatOffentligTransportDto(
        samling = samling.map { it.tilDto() },
        beløp = beløp,
    )

fun BeregningsresultatForSamling.tilDto() =
    BeregningsresultatForSamlingDto(
        reiseId = reiseId,
        adresse = adresse,
        fom = fom,
        tom = tom,
        utgifterOffentligTransport = utgifterOffentligTransport,
    )
