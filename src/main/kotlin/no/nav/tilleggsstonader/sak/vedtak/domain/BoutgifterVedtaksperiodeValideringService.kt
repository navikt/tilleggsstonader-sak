package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.VedtaksperiodeValideringUtils.validerIngenEndringerFørRevurderFra
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.VedtaksperiodeUtil.validerIngenOverlappendeVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.VedtaksperiodeUtil.validerVedtaksperiodeOmfattesAvStønadsperioder
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class BoutgifterVedtaksperiodeValideringService(
    val behandlingService: BehandlingService,
    val vedtakRepository: VedtakRepository,
) {
    fun validerVedtaksperioder(
        vedtaksperioder: List<Vedtaksperiode>,
        stønadsperioder: List<StønadsperiodeBeregningsgrunnlag>,
        behandlingId: BehandlingId,
    ) {
        brukerfeilHvis(vedtaksperioder.isEmpty()) { "Kan ikke innvilge når det ikke finnes noen vedtaksperioder." }
        validerIngenOverlappendeVedtaksperioder(vedtaksperioder)
        validerVedtaksperiodeOmfattesAvStønadsperioder(vedtaksperioder, stønadsperioder)

        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        val vedtaksperioderForrigeBehandling = hentForrigeVedtaksperioder(behandling)

        validerIngenEndringerFørRevurderFra(
            innsendteVedtaksperioder = vedtaksperioder,
            vedtaksperioderForrigeBehandling = vedtaksperioderForrigeBehandling,
            revurderFra = behandling.revurderFra,
        )
    }

    private fun hentForrigeVedtaksperioder(behandling: Saksbehandling): List<Vedtaksperiode>? =
        behandling.forrigeBehandlingId?.let {
            when (val forrigeVedtak = vedtakRepository.findByIdOrNull(it)?.data) {
                is InnvilgelseBoutgifter -> forrigeVedtak.vedtaksperioder
//                is OpphørBoutgifter -> forrigeVedtak.vedtaksperioder
//                is Avslag -> null
                else -> error("Håndterer ikke ${forrigeVedtak?.javaClass?.simpleName}")
            }
        }
}
