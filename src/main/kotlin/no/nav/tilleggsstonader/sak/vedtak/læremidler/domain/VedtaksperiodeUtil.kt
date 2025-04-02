package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import no.nav.tilleggsstonader.kontrakter.felles.førsteOverlappendePeriode
import no.nav.tilleggsstonader.kontrakter.periode.beregnSnitt
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeil
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningsgrunnlagLæremidler

object VedtaksperiodeUtil {
    fun validerIngenOverlappendeVedtaksperioder(vedtaksperioder: List<Vedtaksperiode>) {
        val overlappendePeriode = vedtaksperioder.førsteOverlappendePeriode()
        if (overlappendePeriode != null) {
            brukerfeil(
                "Periode=${overlappendePeriode.first.formatertPeriodeNorskFormat()} og ${overlappendePeriode.second.formatertPeriodeNorskFormat()} overlapper.",
            )
        }
    }

    /**
     * Finner
     */
    fun vedtaksperioderInnenforLøpendeMåned(
        vedtaksperioder: List<VedtaksperiodeBeregningsgrunnlagLæremidler>,
        beregningsresultatTilReberegning: BeregningsresultatForMåned,
    ): List<VedtaksperiodeBeregningsgrunnlagLæremidler> =
        vedtaksperioder
            .mapNotNull { vedtaksperiode ->
                vedtaksperiode.beregnSnitt(beregningsresultatTilReberegning)
            }
}
