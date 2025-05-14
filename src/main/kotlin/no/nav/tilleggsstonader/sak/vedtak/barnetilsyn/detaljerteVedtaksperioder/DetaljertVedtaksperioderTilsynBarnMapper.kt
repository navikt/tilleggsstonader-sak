package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørTilsynBarn

object DetaljertVedtaksperioderTilsynBarnMapper {
    fun InnvilgelseEllerOpphørTilsynBarn.finnDetaljerteVedtaksperioder(): List<DetaljertVedtaksperiodeTilsynBarn> {
        val vedtaksperioderFraBeregningsresultat: List<DetaljertVedtaksperiodeTilsynBarn> =
            finnVedtaksperioderFraBeregningsresultatTilsynBarn(this.beregningsresultat)

        return vedtaksperioderFraBeregningsresultat.sorterOgMergeSammenhengende()
    }

    private fun finnVedtaksperioderFraBeregningsresultatTilsynBarn(beregningsresultatTilsynBarn: BeregningsresultatTilsynBarn) =
        beregningsresultatTilsynBarn.perioder.flatMap { resultatMåned ->
            resultatMåned.grunnlag.vedtaksperiodeGrunnlag
                .map { vedtaksperiodeGrunnlag ->
                    DetaljertVedtaksperiodeTilsynBarn(
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
