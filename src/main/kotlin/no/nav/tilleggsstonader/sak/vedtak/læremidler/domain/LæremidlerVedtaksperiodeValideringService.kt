package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.Avslag
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeUtil.validerIngenOverlappendeVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.validerIngenEndringerFørRevurderFra
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class LæremidlerVedtaksperiodeValideringService(
    val behandlingService: BehandlingService,
    val vedtakRepository: VedtakRepository,
    val unleashService: UnleashService,
) {
    fun validerVedtaksperioder(
        vedtaksperioder: List<Vedtaksperiode>,
        behandlingId: BehandlingId,
    ) {
        brukerfeilHvis(vedtaksperioder.isEmpty()) { "Kan ikke innvilge når det ikke finnes noen vedtaksperioder." }
        validerMålgruppeAktivitetErSatt(vedtaksperioder)
        validerIngenOverlappendeVedtaksperioder(vedtaksperioder)

        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        val vedtaksperioderForrigeBehandling = hentForrigeVedtaksperioder(behandling)

        validerIngenEndringerFørRevurderFra(
            innsendteVedtaksperioder = vedtaksperioder,
            vedtaksperioderForrigeBehandling = vedtaksperioderForrigeBehandling,
            revurderFra = behandling.revurderFra,
        )
    }

    private fun validerMålgruppeAktivitetErSatt(vedtaksperioder: List<Vedtaksperiode>) {
        if (unleashService.isEnabled(Toggle.LÆREMIDLER_VEDTAKSPERIODER_V2)) {
            feilHvis(vedtaksperioder.any { it.målgruppe == null && it.aktivitet == null }) {
                "Refresh siden. Må sette målgruppe og aktivitet på vedtaksperioder."
            }
        } else {
            feilHvis(vedtaksperioder.any { it.målgruppe != null && it.aktivitet != null }) {
                "Refresh siden. Må ikke sette målgruppe og aktivitet på vedtaksperioder."
            }
        }
    }

    private fun hentForrigeVedtaksperioder(behandling: Saksbehandling): List<Vedtaksperiode>? =
        behandling.forrigeIverksatteBehandlingId?.let {
            when (val forrigeVedtak = vedtakRepository.findByIdOrNull(it)?.data) {
                is InnvilgelseLæremidler -> forrigeVedtak.vedtaksperioder
                is OpphørLæremidler -> forrigeVedtak.vedtaksperioder
                is Avslag -> null
                else -> error("Håndterer ikke ${forrigeVedtak?.javaClass?.simpleName}")
            }
        }
}
