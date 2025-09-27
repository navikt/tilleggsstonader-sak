package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.opplysninger.søknad.dagligReise.BillettType
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.VedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate
import java.util.UUID

fun lagBeregningsresultatForReise(
    fom: LocalDate,
    beløp: Int = 100,
    beregningsgrunnlag: Beregningsgrunnlag = lagBeregningsgrunnlag(fom),
    billetDetajler: Map<BillettType, Int> = emptyMap(),
): BeregningsresultatForReise =
    BeregningsresultatForReise(
        perioder =
            listOf(
                BeregningsresultatForPeriode(
                    grunnlag = beregningsgrunnlag,
                    beløp = beløp,
                    billetDetalijer = billetDetajler,
                ),
            ),
    )

fun lagVedtaksperiodeGrunnlag(
    fom: LocalDate,
    målgruppe: FaktiskMålgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
) = VedtaksperiodeGrunnlag(
    id = UUID.randomUUID(),
    fom = fom,
    tom = fom,
    målgruppe = målgruppe,
    aktivitet = AktivitetType.TILTAK,
    antallReisedagerIVedtaksperioden = 5,
)

fun lagBeregningsgrunnlag(
    fom: LocalDate,
    vedtaksperioder: List<VedtaksperiodeGrunnlag> = listOf(lagVedtaksperiodeGrunnlag(fom)),
): Beregningsgrunnlag =
    Beregningsgrunnlag(
        fom = fom,
        tom = fom.plusWeeks(1),
        prisEnkeltbillett = 50,
        prisSyvdagersbillett = null,
        pris30dagersbillett = 1000,
        antallReisedagerPerUke = 5,
        vedtaksperioder = vedtaksperioder,
        antallReisedager = 20,
    )
