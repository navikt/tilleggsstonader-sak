package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.kontrakter.periode.avkortFraOgMed
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vedtak.ForeslåVedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.takeIfType
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
class VedtaksperiodeService(
    private val vilkårperiodeService: VilkårperiodeService,
    private val vilkårService: VilkårService,
    @Lazy // For å unngå sirkulær avhengighet i spring
    private val vedtakService: VedtakService,
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

        val forrigeVedtak = vedtakService.hentVedtak(behandling.forrigeBehandlingId)?.data
        val forrigeVedtaksperioder =
            when (forrigeVedtak) {
                is InnvilgelseTilsynBarn -> forrigeVedtak.vedtaksperioder
                is OpphørTilsynBarn -> forrigeVedtak.vedtaksperioder
                else -> error("Vi støtter ikke opphør for tilsyn barn når forrige vedtak var ${forrigeVedtak?.javaClass?.simpleName}")
            }

        feilHvis(forrigeVedtaksperioder == null) {
            "Kan ikke opphøre fordi data fra forrige vedtak mangler"
        }

        return forrigeVedtaksperioder.avkortFraOgMed(behandling.revurderFra.minusDays(1))
    }

    private fun detFinnesVedtaksperioder(behandlingId: BehandlingId) =
        vedtakService
            .hentVedtak(behandlingId)
            ?.takeIfType<InnvilgelseTilsynBarn>()
            ?.data
            ?.vedtaksperioder
            .isNullOrEmpty()
            .not()
}
