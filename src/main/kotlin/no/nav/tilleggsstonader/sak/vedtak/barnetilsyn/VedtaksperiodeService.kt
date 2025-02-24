package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.kontrakter.periode.avkortFraOgMed
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
) {
    fun foreslåPerioder(behandlingId: BehandlingId): List<Vedtaksperiode> {
        brukerfeilHvis(detFinnesVedtaksperioder(behandlingId)) {
            "Det finnes allerede lagrede vedtaksperioder for denne behandlingen"
        }

        val vilkårperioder = vilkårperiodeService.hentVilkårperioder(behandlingId)
        val vilkår = vilkårService.hentVilkår(behandlingId)

        return ForeslåVedtaksperiode.finnVedtaksperiode(
            vilkårperioder = vilkårperioder,
            vilkår = vilkår,
        )
    }

    fun finnNyeVedtaksperioderForOpphør(behandling: Saksbehandling): List<Vedtaksperiode> {
        brukerfeilHvis(behandling.forrigeBehandlingId == null) {
            "Kan ikke finne nye vedtaksperioder for opphør fordi behandlingen er en førstegangsbehandling"
        }
        brukerfeilHvis(behandling.revurderFra == null) {
            "Kan ikke finne nye vedtaksperioder for opphør fordi revurder fra dato mangler"
        }

        val forrigeVedtaksperioder = finnVedtaksperioder(behandling.forrigeBehandlingId)

        feilHvis(forrigeVedtaksperioder == null) {
            "Kan ikke opphøre fordi data fra forrige vedtak mangler"
        }

        // .minusDays(1) fordi dagen før revurder fra blir siste dag i vedtaksperioden
        return forrigeVedtaksperioder.avkortFraOgMed(behandling.revurderFra.minusDays(1))
    }

    private fun detFinnesVedtaksperioder(behandlingId: BehandlingId) = !finnVedtaksperioder(behandlingId).isNullOrEmpty()

    private fun finnVedtaksperioder(behandlingId: BehandlingId): List<Vedtaksperiode>? {
        val forrigeVedtak = vedtakRepository.findByIdOrNull(behandlingId)?.data
        return when (forrigeVedtak) {
            null -> null
            is InnvilgelseTilsynBarn -> forrigeVedtak.vedtaksperioder
            is OpphørTilsynBarn -> forrigeVedtak.vedtaksperioder
            else ->
                error(
                    "Kan ikke hente forrgie vedtaksperioder for tilsyn barn når forrgie vedtak var ${forrigeVedtak.javaClass.simpleName}",
                )
        }
    }
}
