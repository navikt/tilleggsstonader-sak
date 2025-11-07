package no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise

object DetaljertVedtaksperioderDagligReiseMapper {
    fun InnvilgelseEllerOpphørDagligReise.finnDetaljerteVedtaksperioderTso(): List<DetaljertVedtaksperiodeDagligReiseTso> {
        val vedaksperioderFraBeregningsresultat =
            this.beregningsresultat.offentligTransport?.reiser?.flatMap { reise ->
                reise.perioder.flatMap { periode ->
                    periode.grunnlag.vedtaksperioder.map { vedtaksperiode ->
                        DetaljertVedtaksperiodeDagligReiseTso(
                            fom = vedtaksperiode.fom,
                            tom = vedtaksperiode.tom,
                            aktivitet = vedtaksperiode.aktivitet,
                            målgruppe = vedtaksperiode.målgruppe,
                            typeDagligReise = TypeDagligReise.OFFENTLIG_TRANSPORT,
                        )
                    }
                }
            }
        return vedaksperioderFraBeregningsresultat?.sorterOgMergeSammenhengende()
            ?: error("Fant ikke vedtsksperioder for offentlig transport.")
    }
}
