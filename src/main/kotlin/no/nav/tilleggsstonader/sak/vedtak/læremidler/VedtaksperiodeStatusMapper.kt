package no.nav.tilleggsstonader.sak.vedtak.læremidler

import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.VedtaksperiodeStatus

object VedtaksperiodeStatusMapper {
    fun settStatusPåVedtaksperioder(
        vedtaksperioder: List<Vedtaksperiode>,
        vedtaksperioderForrigeBehandling: List<Vedtaksperiode>?,
    ): List<Vedtaksperiode> {
        if (vedtaksperioderForrigeBehandling.isNullOrEmpty()) {
            return vedtaksperioder.map { it.copy(status = VedtaksperiodeStatus.NY) }
        }

        val vedtaksperiodeIdforidForForrigeBehandling =
            vedtaksperioderForrigeBehandling.map { it.id }

        val nyeVedtakPerioder = vedtaksperioder.filterNot { it.id in vedtaksperiodeIdforidForForrigeBehandling }

        val endretdeVedtakPerioder =
            vedtaksperioder.filter { vedtaksperiode ->
                vedtaksperiode.id in vedtaksperiodeIdforidForForrigeBehandling &&
                    vedtaksperioderForrigeBehandling
                        .find { it.id == vedtaksperiode.id }
                        ?.let { it.fom != vedtaksperiode.fom || it.tom != vedtaksperiode.tom } == true
            }
        val endretVedtakPerioderMedEndretStatus =
            endretdeVedtakPerioder.map {
                it.copy(status = VedtaksperiodeStatus.ENDRET)
            }

        val uendretVedtaksperioder =
            vedtaksperioder.filter { vedtaksperiode ->
                vedtaksperiode.id in vedtaksperiodeIdforidForForrigeBehandling &&
                    vedtaksperioderForrigeBehandling
                        .find { it.id == vedtaksperiode.id }
                        ?.let { it.fom == vedtaksperiode.fom && it.tom == vedtaksperiode.tom } == true
            }
        val uendretVedtakPerioderMedEndretStatus =
            uendretVedtaksperioder.map {
                it.copy(status = VedtaksperiodeStatus.UENDRET)
            }

        return nyeVedtakPerioder + endretVedtakPerioderMedEndretStatus + uendretVedtakPerioderMedEndretStatus
    }
}
