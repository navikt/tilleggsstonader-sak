package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
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
): BeregningsresultatForReise =
    BeregningsresultatForReise(
        perioder =
            listOf(
                BeregningsresultatForPeriode(
                    grunnlag = lagBeregningsgrunnlag(fom),
                    beløp = beløp,
                ),
            ),
    )

fun lagBeregningsgrunnlag(fom: LocalDate): Beregningsgrunnlag =
    Beregningsgrunnlag(
        fom = fom,
        tom = fom.plusWeeks(1),
        prisEnkeltbillett = 50,
        pris30dagersbillett = 1000,
        antallReisedagerPerUke = 5,
        vedtaksperioder =
            listOf(
                VedtaksperiodeGrunnlag(
                    id = UUID.randomUUID(),
                    fom = fom,
                    tom = fom,
                    målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                    aktivitet = AktivitetType.TILTAK,
                    antallReisedagerIVedtaksperioden = 5,
                ),
            ),
        antallReisedager = 20,
    )
