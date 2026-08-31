package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling

import no.nav.tilleggsstonader.sak.felles.domain.VedtaksperiodeId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.*
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import java.math.BigDecimal
import java.time.LocalDate

fun lagVedtaksperiodeGrunnlag(fom: LocalDate, tom: LocalDate) =
    VedtaksperiodeGrunnlag(
        id = VedtaksperiodeId.random(),
        fom = fom,
        tom = tom,
        målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
        aktivitet = AktivitetType.TILTAK,
    )

fun lagBeregningsresultatForOffentligTransport(
    fom: LocalDate,
    tom: LocalDate = fom,
    reiseId: ReiseId = ReiseId.random(),
    beløp: BigDecimal = BigDecimal.ZERO,
): BeregningsresultatOffentligTransport {
    val grunnlag = BeregningsgrunnlagOffentligTransportForSamling(
        adresse = null,
        fom = fom,
        tom = tom,
        vedtaksperioder = listOf(lagVedtaksperiodeGrunnlag(fom, tom)),
        brukersNavKontor = null,
    )

    return BeregningsresultatOffentligTransport(
        reiseId = reiseId,
        grunnlag = grunnlag,
        beløp = beløp,
    )
}

fun lagBeregningsresultatForPrivatBil(
    fom: LocalDate,
    tom: LocalDate = fom,
    reiseId: ReiseId = ReiseId.random(),
    beløp: BigDecimal = BigDecimal.ZERO,
): BeregningsresultatPrivatBil {
    val grunnlag = BeregningsgrunnlagPrivatBilForSamling(
        adresse = null,
        fom = fom,
        tom = tom,
        sats = BigDecimal.ZERO,
        totaltReiseavstand = BigDecimal.ZERO,
        vedtaksperioder = listOf(lagVedtaksperiodeGrunnlag(fom, tom)),
        brukersNavKontor = null,
    )

    return BeregningsresultatPrivatBil(
        reiseId = reiseId,
        grunnlag = grunnlag,
        beløp = beløp,
    )
}
