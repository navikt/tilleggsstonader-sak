package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.beregning.BeregningReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatOffentligTransportForSamling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import java.time.LocalDate

data class BeregningsresultatReiseTilSamlingDto(
    val offentligTransport: BeregningsresultatOffentligTransportDto?,
)

data class BeregningsresultatOffentligTransportDto(
    val reiser: List<BeregningsresultatForSamlingDto>,
)

data class BeregningsresultatForSamlingDto(
    val reiseId: ReiseId,
    val adresse: String?,
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: Int?,
)

data class BeregningsresultatForPeriodeOffentligTransportDto(
    override val fom: LocalDate,
    override val tom: LocalDate,
) : Periode<LocalDate>

fun BeregningReiseTilSamling.tilDto(): BeregningsresultatReiseTilSamlingDto =
    BeregningsresultatReiseTilSamlingDto(
        offentligTransport = beregningsresultatOffentligTransport.tilDto(),
    )

fun BeregningsresultatOffentligTransport.tilDto() =
    BeregningsresultatOffentligTransportDto(
        reiser = reiser.map { it.tilDto() },
    )

fun BeregningsresultatOffentligTransportForSamling.tilDto() =
    BeregningsresultatForSamlingDto(
        reiseId = reiseId,
        adresse = grunnlag.adresse,
        fom = grunnlag.fom,
        tom = grunnlag.tom,
        beløp = beløp,
    )
