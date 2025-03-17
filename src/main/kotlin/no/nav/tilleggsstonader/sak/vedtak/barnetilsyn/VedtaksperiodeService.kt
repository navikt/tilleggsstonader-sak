package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.kontrakter.periode.avkortFraOgMed
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vedtak.ForeslåVedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class VedtaksperiodeService(
    private val vilkårperiodeService: VilkårperiodeService,
    private val vilkårService: VilkårService,
    private val vedtakRepository: VedtakRepository,
    private val behandlingService: BehandlingService,
) {
    fun foreslåPerioder(behandlingId: BehandlingId): List<Vedtaksperiode> {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        brukerfeilHvis(detFinnesVedtaksperioderPåForrigeBehandling(saksbehandling)) {
            "Kan ikke foreslå vedtaksperioder fordi det finnes lagrede vedtaksperioder fra en tidligere behandling"
        }

        val vilkårperioder = vilkårperiodeService.hentVilkårperioder(behandlingId)
        val vilkår = vilkårService.hentVilkår(behandlingId)

        return ForeslåVedtaksperiode.finnVedtaksperiode(
            vilkårperioder = vilkårperioder,
            vilkår = vilkår,
        )
    }

    fun finnNyeVedtaksperioderForOpphør(behandling: Saksbehandling): List<Vedtaksperiode> {
        feilHvis(behandling.forrigeIverksatteBehandlingId == null) {
            "Kan ikke finne nye vedtaksperioder for opphør fordi behandlingen er en førstegangsbehandling"
        }
        feilHvis(behandling.revurderFra == null) {
            "Kan ikke finne nye vedtaksperioder for opphør fordi revurder fra dato mangler"
        }

        val forrigeVedtaksperioder = finnVedtaksperioder(behandling.forrigeIverksatteBehandlingId)

        feilHvis(forrigeVedtaksperioder == null) {
            "Kan ikke opphøre fordi data fra forrige vedtak mangler"
        }

        // .minusDays(1) fordi dagen før revurder fra blir siste dag i vedtaksperioden
        return forrigeVedtaksperioder.avkortFraOgMed(behandling.revurderFra.minusDays(1))
    }

    fun detFinnesVedtaksperioderPåForrigeBehandling(saksbehandling: Saksbehandling): Boolean =
        finnVedtaksperioder(saksbehandling.forrigeIverksatteBehandlingId)?.isNotEmpty() == true

    private fun finnVedtaksperioder(behandlingId: BehandlingId?): List<Vedtaksperiode>? {
        if (behandlingId == null) return null
        val vedtak = vedtakRepository.findByIdOrNull(behandlingId)?.data
        return when (vedtak) {
            null -> null
            is InnvilgelseTilsynBarn -> vedtak.vedtaksperioder
            is OpphørTilsynBarn -> vedtak.vedtaksperioder
            else ->
                error(
                    "Kan ikke hente vedtaksperioder for tilsyn barn når vedtak var ${vedtak.type}",
                )
        }
    }
}
