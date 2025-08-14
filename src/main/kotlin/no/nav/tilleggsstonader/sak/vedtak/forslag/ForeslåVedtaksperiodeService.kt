package no.nav.tilleggsstonader.sak.vedtak.forslag

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tidligsteendring.UtledTidligsteEndringService
import no.nav.tilleggsstonader.sak.vedtak.VedtaksperiodeService
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.springframework.stereotype.Service

/**
 * TODO 1 - Burde forholde seg til arenaTom når man foreslår vedtaksperioder
 */
@Service
class ForeslåVedtaksperiodeService(
    private val behandlingService: BehandlingService,
    private val vilkårperiodeService: VilkårperiodeService,
    private val vilkårService: VilkårService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val utledTidligsteEndringService: UtledTidligsteEndringService,
) {
    fun foreslåPerioder(behandlingId: BehandlingId): List<Vedtaksperiode> {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)

        val vilkårperioder = vilkårperiodeService.hentVilkårperioder(saksbehandling.id)
        val forrigeVedtaksperioder =
            saksbehandling.forrigeIverksatteBehandlingId?.let {
                // skal hente alle vedtaksperioder fra forrige behandling, så setter revurderFra til null
                vedtaksperiodeService.finnVedtaksperioderForBehandling(it, revurdererFra = null)
            } ?: emptyList()
        val tidligsteEndring =
            utledTidligsteEndringService.utledTidligsteEndringIgnorerVedtaksperioder(saksbehandling.id)
        return if (saksbehandling.stønadstype.skalHenteStønadsvilkår()) {
            ForeslåVedtaksperiode.finnVedtaksperiode(
                vilkårperioder = vilkårperioder,
                vilkår = vilkårService.hentVilkår(saksbehandling.id),
                forrigeVedtaksperioder = forrigeVedtaksperioder,
                tidligsteEndring = tidligsteEndring,
            )
        } else {
            ForeslåVedtaksperiode.finnVedtaksperiodeUtenVilkår(
                vilkårperioder = vilkårperioder,
                forrigeVedtaksperioder = forrigeVedtaksperioder,
                tidligsteEndring = tidligsteEndring,
            )
        }
    }

    private fun Stønadstype.skalHenteStønadsvilkår(): Boolean =
        when (this) {
            Stønadstype.LÆREMIDLER -> false
            Stønadstype.BARNETILSYN -> true
            Stønadstype.BOUTGIFTER -> true
            Stønadstype.DAGLIG_REISE_TSO -> true
            Stønadstype.DAGLIG_REISE_TSR -> true
        }
}
