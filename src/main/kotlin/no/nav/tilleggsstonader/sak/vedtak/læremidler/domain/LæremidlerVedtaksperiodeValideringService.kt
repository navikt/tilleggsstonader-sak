package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.VedtaksperiodeValideringUtils.validerAtVedtaksperioderIkkeOverlapperMedVilkårPeriodeUtenRett
import no.nav.tilleggsstonader.sak.vedtak.VedtaksperiodeValideringUtils.validerEnkeltperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.Avslag
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeUtil.validerIngenOverlappendeVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.validerIngenEndringerFørRevurderFra
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.mergeSammenhengendeOppfylteAktiviteter
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.mergeSammenhengendeOppfylteFaktiskeMålgrupper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class LæremidlerVedtaksperiodeValideringService(
    val behandlingService: BehandlingService,
    val vedtakRepository: VedtakRepository,
    val vilkårperiodeService: VilkårperiodeService,
) {
    fun validerVedtaksperioder(
        vedtaksperioder: List<Vedtaksperiode>,
        behandlingId: BehandlingId,
    ) {
        brukerfeilHvis(vedtaksperioder.isEmpty()) { "Kan ikke innvilge når det ikke finnes noen vedtaksperioder." }
        validerIngenOverlappendeVedtaksperioder(vedtaksperioder)

        validerVedtaksperioderMotVilkårperioder(behandlingId, vedtaksperioder)

        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        val vedtaksperioderForrigeBehandling = hentForrigeVedtaksperioder(behandling)

        validerIngenEndringerFørRevurderFra(
            innsendteVedtaksperioder = vedtaksperioder,
            vedtaksperioderForrigeBehandling = vedtaksperioderForrigeBehandling,
            revurderFra = behandling.revurderFra,
        )
    }

    private fun validerVedtaksperioderMotVilkårperioder(
        behandlingId: BehandlingId,
        vedtaksperioder: List<Vedtaksperiode>,
    ) {
        val vilkårperioder = vilkårperiodeService.hentVilkårperioder(behandlingId)
        validerAtVedtaksperioderIkkeOverlapperMedVilkårPeriodeUtenRett(vilkårperioder, vedtaksperioder)

        val målgrupper = vilkårperioder.målgrupper.mergeSammenhengendeOppfylteFaktiskeMålgrupper()
        val aktiviteter = vilkårperioder.aktiviteter.mergeSammenhengendeOppfylteAktiviteter()
        vedtaksperioder.forEach {
            validerEnkeltperiode(
                vedtaksperiode = it,
                målgruppePerioderPerType = målgrupper,
                aktivitetPerioderPerType = aktiviteter,
            )
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
