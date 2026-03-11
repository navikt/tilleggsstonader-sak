package no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise

object DetaljertVedtaksperioderDagligReiseMapper {
    fun finnDetaljerteVedtaksperioderDagligReise(
        vedtaksdataTso: InnvilgelseEllerOpphørDagligReise?,
        vedtaksdataTsr: InnvilgelseEllerOpphørDagligReise?,
    ): List<DetaljertVedtaksperiodeDagligReise> {
        val alleReisePerioderTso = vedtaksdataTso?.tilReiseperioder()
        val alleReisePerioderTsr = vedtaksdataTsr?.tilReiseperioder()

        val beregningsresultatDetaljeertForDagligReiseTSO =
            alleReisePerioderTso?.tilDetaljertBeregningsperioder(Stønadstype.DAGLIG_REISE_TSO)
        val beregningsresultatDetaljeertForDagligReiseTSR =
            alleReisePerioderTsr?.tilDetaljertBeregningsperioder(Stønadstype.DAGLIG_REISE_TSR)
        val beregningsDetaljertForDagligReise =
            listOf(
                beregningsresultatDetaljeertForDagligReiseTSO.orEmpty(),
                beregningsresultatDetaljeertForDagligReiseTSR.orEmpty(),
            ).flatten()
        return beregningsDetaljertForDagligReise
    }

    private fun InnvilgelseEllerOpphørDagligReise.tilReiseperioder() =
        beregningsresultat.offentligTransport?.reiser?.flatMap { reise ->
            reise.perioder
        }

    private fun List<BeregningsresultatForPeriode>.tilDetaljertBeregningsperioder(
        stønadstype: Stønadstype,
    ): List<DetaljertVedtaksperiodeDagligReise> {
        val detaljertBeregningsperioder =
            this.sortedBy { it.grunnlag.fom }.map { beregningsresultatForPeriode ->
                DetaljertBeregningsperioder(
                    fom = beregningsresultatForPeriode.grunnlag.fom,
                    tom = beregningsresultatForPeriode.grunnlag.tom,
                    prisEnkeltbillett = beregningsresultatForPeriode.grunnlag.prisEnkeltbillett,
                    prisSyvdagersbillett = beregningsresultatForPeriode.grunnlag.prisSyvdagersbillett,
                    pris30dagersbillett = beregningsresultatForPeriode.grunnlag.pris30dagersbillett,
                    beløp = beregningsresultatForPeriode.beløp,
                    billettdetaljer = beregningsresultatForPeriode.billettdetaljer,
                    antallReisedager = beregningsresultatForPeriode.grunnlag.antallReisedager,
                    antallReisedagerPerUke = beregningsresultatForPeriode.grunnlag.antallReisedagerPerUke,
                )
            }

        return listOf(
            DetaljertVedtaksperiodeDagligReise(
                stønadstype = stønadstype,
                typeDagligReise = TypeDagligReise.OFFENTLIG_TRANSPORT,
                detaljertBeregningsperioder = detaljertBeregningsperioder,
            ),
        )
    }
}
