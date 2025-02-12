package no.nav.tilleggsstonader.sak.vedtak.læremidler

import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeStatus

object VedtaksperiodeStatusMapper {
    fun settStatusPåVedtaksperioder(
        vedtaksperioder: List<Vedtaksperiode>,
        vedtaksperioderForrigeBehandling: List<Vedtaksperiode>?,
    ): List<Vedtaksperiode> =
        vedtaksperioder.map { vedtaksperiode ->
            val forrigeVedtaksperiode = vedtaksperioderForrigeBehandling?.find { it.id == vedtaksperiode.id }
            vedtaksperiode.copy(status = vedtaksperiode.finnNyStatus(forrigeVedtaksperiode))
        }

    private fun Vedtaksperiode.finnNyStatus(forrigeVedtaksperiode: Vedtaksperiode?): VedtaksperiodeStatus =
        when {
            forrigeVedtaksperiode == null -> VedtaksperiodeStatus.NY
            this.fom == forrigeVedtaksperiode.fom && this.tom == forrigeVedtaksperiode.tom ->
                VedtaksperiodeStatus.UENDRET

            else -> VedtaksperiodeStatus.ENDRET
        }
}
