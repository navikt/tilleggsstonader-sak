package no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.Billettype
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatDagligReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatOffentligTransport
import java.time.LocalDate

data class BeregningsresultatDagligReiseDto(
    val offentligTransport: BeregningsresultatOffentligTransportDto?,
    val tidligsteEndring: LocalDate? = null,
)

data class BeregningsresultatOffentligTransportDto(
    val reiser: List<BeregningsresultatForReiseDto>,
)

data class BeregningsresultatForReiseDto(
    val perioder: List<BeregningsresultatForPeriodeDto>,
)

data class BeregningsresultatForPeriodeDto(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val prisEnkeltbillett: Int?,
    val prisSyvdagersbillett: Int?,
    val pris30dagersbillett: Int?,
    val antallReisedagerPerUke: Int,
    val antallReisedager: Int,
    val beløp: Int,
    val billettdetaljer: Map<Billettype, Int>,
) : Periode<LocalDate>

fun BeregningsresultatDagligReise.tilDto(tidligsteEndring: LocalDate?): BeregningsresultatDagligReiseDto =
    BeregningsresultatDagligReiseDto(
        offentligTransport = offentligTransport?.tilDto(),
        tidligsteEndring = tidligsteEndring,
    )

fun BeregningsresultatOffentligTransport.tilDto(): BeregningsresultatOffentligTransportDto =
    BeregningsresultatOffentligTransportDto(
        reiser = reiser.map { it.tilDto() },
    )

fun BeregningsresultatForReise.tilDto(): BeregningsresultatForReiseDto =
    BeregningsresultatForReiseDto(
        perioder = perioder.map { it.tilDto() },
    )

fun BeregningsresultatForPeriode.tilDto(): BeregningsresultatForPeriodeDto =
    BeregningsresultatForPeriodeDto(
        fom = grunnlag.fom,
        tom = grunnlag.tom,
        prisEnkeltbillett = grunnlag.prisEnkeltbillett,
        prisSyvdagersbillett = grunnlag.prisSyvdagersbillett,
        pris30dagersbillett = grunnlag.pris30dagersbillett,
        antallReisedagerPerUke = grunnlag.antallReisedagerPerUke,
        antallReisedager = grunnlag.antallReisedager,
        beløp = beløp,
        billettdetaljer = billettdetaljer,
    )
