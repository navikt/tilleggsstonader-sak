package no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise

object DetaljertVedtaksperioderDagligReiseMapper {
    fun finnDetaljerteVedtaksperioderDagligReise(
        vedtaksdataTso: InnvilgelseEllerOpphørDagligReise?,
        vedtaksdataTsr: InnvilgelseEllerOpphørDagligReise?,
    ): List<DetaljertBeregningsperioderDagligReise> {
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
    ): List<DetaljertBeregningsperioderDagligReise> =
        map { periode ->
            DetaljertBeregningsperioderDagligReise(
                fom = periode.grunnlag.fom,
                tom = periode.grunnlag.tom,
                prisEnkeltbillett = periode.grunnlag.prisEnkeltbillett,
                prisSyvdagersbillett = periode.grunnlag.prisSyvdagersbillett,
                pris30dagersbillett = periode.grunnlag.pris30dagersbillett,
                beløp = periode.beløp,
                billettdetaljer = periode.billettdetaljer,
                antallReisedager = periode.grunnlag.antallReisedager,
                stønadstype = stønadstype,
                antallReisedagerPerUke = periode.grunnlag.antallReisedagerPerUke,
                typeDagligReise = TypeDagligReise.OFFENTLIG_TRANSPORT,
            )
        }
}
