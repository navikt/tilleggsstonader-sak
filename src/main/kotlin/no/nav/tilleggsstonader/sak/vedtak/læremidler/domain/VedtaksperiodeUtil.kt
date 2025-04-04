package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import no.nav.tilleggsstonader.kontrakter.periode.beregnSnitt
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning

object VedtaksperiodeUtil {
    /**
     * Finner
     */
    fun vedtaksperioderInnenforLøpendeMåned(
        vedtaksperioder: List<VedtaksperiodeBeregning>,
        beregningsresultatTilReberegning: BeregningsresultatForMåned,
    ): List<VedtaksperiodeBeregning> =
        vedtaksperioder
            .mapNotNull { vedtaksperiode ->
                vedtaksperiode.beregnSnitt(beregningsresultatTilReberegning)
            }
}
