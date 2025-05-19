package no.nav.tilleggsstonader.sak.vedtak.læremidler.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørLæremidler

object DetaljertVedtaksperioderLæremidlerMapper {
    fun InnvilgelseEllerOpphørLæremidler.finnDetaljerteVedtaksperioder(): List<DetaljertVedtaksperiodeLæremidler> {
        val vedtaksperioderFraBeregningsresultat =
            this.beregningsresultat.perioder.map { periode ->
                DetaljertVedtaksperiodeLæremidler(
                    fom = periode.fom,
                    tom = periode.tom,
                    antallMåneder = 1,
                    aktivitet = periode.grunnlag.aktivitet,
                    målgruppe = periode.grunnlag.målgruppe,
                    studienivå = periode.grunnlag.studienivå,
                    studieprosent = periode.grunnlag.studieprosent,
                    månedsbeløp = periode.beløp,
                )
            }

        return vedtaksperioderFraBeregningsresultat.sorterOgMergeSammenhengende()
    }
}
