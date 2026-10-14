package no.nav.tilleggsstonader.sak.vedtak.forslag

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.GrunnlagForVedtaksperiode
import no.nav.tilleggsstonader.sak.felles.domain.grunnlagForVedtaksperiode
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
        return when (saksbehandling.stønadstype.grunnlagForVedtaksperiode()) {
            GrunnlagForVedtaksperiode.MÅLGRUPPE_AKTIVITET_OG_VILKÅR ->
                ForeslåVedtaksperiode.finnVedtaksperiode(
                    vilkårperioder = vilkårperioder,
                    vilkår = vilkårService.hentVilkår(saksbehandling.id),
                    forrigeVedtaksperioder = forrigeVedtaksperioder,
                    tidligsteEndring = tidligsteEndring,
                )

            GrunnlagForVedtaksperiode.MÅLGRUPPE_OG_VILKÅR ->
                ForeslåVedtaksperiode.finnVedtaksperiodeUtenAktivitet(
                    vilkårperioder = vilkårperioder,
                    vilkår = vilkårService.hentVilkår(saksbehandling.id),
                    forrigeVedtaksperioder = forrigeVedtaksperioder,
                    tidligsteEndring = tidligsteEndring,
                )

            GrunnlagForVedtaksperiode.MÅLGRUPPE_OG_AKTIVITET ->
                ForeslåVedtaksperiode.finnVedtaksperiodeUtenVilkår(
                    vilkårperioder = vilkårperioder,
                    forrigeVedtaksperioder = forrigeVedtaksperioder,
                    tidligsteEndring = tidligsteEndring,
                )
        }
    }
}
