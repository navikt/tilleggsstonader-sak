package no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.Billettype
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatDagligReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import java.time.LocalDate

data class BeregningsresultatDagligReiseDto(
    val offentligTransport: BeregningsresultatOffentligTransportDto?,
    val privatBil: BeregningsresultatPrivatBil? = null,
    val tidligsteEndring: LocalDate? = null,
)

data class BeregningsresultatOffentligTransportDto(
    val reiser: List<BeregningsresultatForReiseDto>,
)

data class BeregningsresultatForReiseDto(
    val reiseId: ReiseId,
    val perioder: List<BeregningsresultatForPeriodeDto>,
)

data class BeregningsresultatForPeriodeDto(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val prisEnkeltbillett: Int?,
    val prisSyvdagersbillett: Int?,
    val pris30dagersbillett: Int?,
    val antallReisedagerPerUke: Int,
    val beløp: Int,
    val billettdetaljer: Map<Billettype, Int>,
    val antallReisedager: Int,
    val fraTidligereVedtak: Boolean,
    val brukersNavKontor: String?,
) : Periode<LocalDate>

fun BeregningsresultatDagligReise.tilDto(tidligsteEndring: LocalDate?): BeregningsresultatDagligReiseDto =
    BeregningsresultatDagligReiseDto(
        offentligTransport = offentligTransport?.tilDto(),
        privatBil = privatBil,
        tidligsteEndring = tidligsteEndring,
    )

fun BeregningsresultatOffentligTransport.tilDto(): BeregningsresultatOffentligTransportDto =
    BeregningsresultatOffentligTransportDto(
        reiser = reiser.map { it.tilDto() },
    )

fun BeregningsresultatForReise.tilDto(): BeregningsresultatForReiseDto =
    BeregningsresultatForReiseDto(
        reiseId = reiseId,
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
        beløp = beløp,
        billettdetaljer = billettdetaljer,
        antallReisedager = grunnlag.antallReisedager,
        fraTidligereVedtak = fraTidligereVedtak,
        brukersNavKontor = grunnlag.brukersNavKontor,
    )
