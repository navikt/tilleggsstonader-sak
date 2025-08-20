package no.nav.tilleggsstonader.sak.vedtak.forslag

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tidligsteendring.UtledTidligsteEndringService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
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
    private val vedtakService: VedtakService,
    private val utledTidligsteEndringService: UtledTidligsteEndringService,
) {
    fun foreslåPerioder(behandlingId: BehandlingId): List<Vedtaksperiode> {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)

        val vilkårperioder = vilkårperiodeService.hentVilkårperioder(saksbehandling.id)
        val forrigeVedtaksperioder =
            saksbehandling.forrigeIverksatteBehandlingId?.let {
                vedtakService.hentVedtaksperioder(it)
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
