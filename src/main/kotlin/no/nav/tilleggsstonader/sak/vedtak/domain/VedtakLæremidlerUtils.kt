package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode

object VedtakLæremidlerUtils {
    fun Vedtak.finnVedtaksperioder(): List<Vedtaksperiode>? =
        when (this.data) {
            is InnvilgelseLæremidler -> this.data.vedtaksperioder
            is OpphørLæremidler -> this.data.vedtaksperioder
            else -> null
        }
}
