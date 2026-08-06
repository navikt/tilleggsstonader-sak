package no.nav.tilleggsstonader.sak.vedtak.passAvBarn.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørPassAvBarn
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.domain.BeregningsresultatPassAvBarn

object DetaljertVedtaksperioderPassAvBarnMapper {
    fun InnvilgelseEllerOpphørPassAvBarn.finnDetaljerteVedtaksperioder(): List<DetaljertVedtaksperiodePassAvBarn> {
        val vedtaksperioderFraBeregningsresultat =
            finnVedtaksperioderFraBeregningsresultatPassAvBarn(this.beregningsresultat)

        return vedtaksperioderFraBeregningsresultat.sorterOgMergeSammenhengende().sortedByDescending { it.fom }
    }

    private fun finnVedtaksperioderFraBeregningsresultatPassAvBarn(beregningsresultatPassAvBarn: BeregningsresultatPassAvBarn) =
        beregningsresultatPassAvBarn.perioder.flatMap { resultatMåned ->
            resultatMåned.grunnlag.vedtaksperiodeGrunnlag
                .map { vedtaksperiodeGrunnlag ->
                    DetaljertVedtaksperiodePassAvBarn(
                        fom = vedtaksperiodeGrunnlag.vedtaksperiode.fom,
                        tom = vedtaksperiodeGrunnlag.vedtaksperiode.tom,
                        aktivitet = vedtaksperiodeGrunnlag.vedtaksperiode.aktivitet,
                        målgruppe = vedtaksperiodeGrunnlag.vedtaksperiode.målgruppe,
                        antallBarn = resultatMåned.grunnlag.antallBarn,
                        totalMånedsUtgift = resultatMåned.grunnlag.utgifterTotal,
                    )
                }
        }
}
