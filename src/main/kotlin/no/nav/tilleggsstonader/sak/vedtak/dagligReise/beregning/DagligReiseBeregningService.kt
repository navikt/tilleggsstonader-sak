package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.tidligsteendring.UtledTidligsteEndringService
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatDagligReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.VilkårDagligReiseMapper.mapTilVilkårDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
import org.springframework.stereotype.Service

@Service
class DagligReiseBeregningService(
    private val vilkårService: VilkårService,
    private val vedtaksperiodeValideringService: VedtaksperiodeValideringService,
    private val offentligTransportBeregningService: OffentligTransportBeregningService,
    private val vedtakRepository: VedtakRepository,
    private val offentligTransportBeregningValidering: OffentligTransportBeregningValidering,
    private val utledTidligsteEndringService: UtledTidligsteEndringService,
) {
    fun beregn(
        behandlingId: BehandlingId,
        vedtaksperioder: List<Vedtaksperiode>,
        behandling: Saksbehandling,
        typeVedtak: TypeVedtak,
    ): BeregningsresultatDagligReise {
        vedtaksperiodeValideringService.validerVedtaksperioder(
            vedtaksperioder = vedtaksperioder,
            behandling = behandling,
            typeVedtak = typeVedtak,
        )

        val oppfylteVilkår =
            vilkårService.hentOppfylteDagligReiseVilkår(behandlingId).map { it.mapTilVilkårDagligReise() }
        validerFinnesReiser(oppfylteVilkår)

        val oppfylteVilkårGruppertPåType = oppfylteVilkår.filter { it.fakta != null }.groupBy { it.fakta!!.type }

        return BeregningsresultatDagligReise(
            offentligTransport = beregnOffentligTransport(oppfylteVilkårGruppertPåType, vedtaksperioder, behandling),
        )
    }

    private fun beregnOffentligTransport(
        vilkår: Map<TypeDagligReise, List<VilkårDagligReise>>,
        vedtaksperioder: List<Vedtaksperiode>,
        behandling: Saksbehandling,
    ): BeregningsresultatOffentligTransport? {
        val vilkårOffentligTransport = vilkår[TypeDagligReise.OFFENTLIG_TRANSPORT] ?: return null

        val forrigeVedtak = hentForrigeVedtak(behandling)

        val beregnignsresultat =
            offentligTransportBeregningService.beregn(
                vedtaksperioder = vedtaksperioder,
                oppfylteVilkår = vilkårOffentligTransport,
            )

        if (forrigeVedtak != null) {
            val tidligsteEndring =
                utledTidligsteEndringService.utledTidligsteEndringForBeregning(
                    behandlingId = behandling.id,
                    vedtaksperioder = vedtaksperioder,
                )

            offentligTransportBeregningValidering.validerRevurdering(
                beregnignsresultat = beregnignsresultat,
                tidligsteEndring = tidligsteEndring,
                forrigeVedtak = forrigeVedtak,
            )
        }
        return beregnignsresultat
    }

    private fun validerFinnesReiser(vilkår: List<VilkårDagligReise>) {
        brukerfeilHvis(vilkår.isEmpty()) {
            "Innvilgelse er ikke et gyldig vedtaksresultat når det ikke er lagt inn perioder med reise"
        }
    }

    private fun hentVedtak(behandlingId: BehandlingId) =
        vedtakRepository
            .findByIdOrThrow(behandlingId)
            .withTypeOrThrow<InnvilgelseEllerOpphørDagligReise>()

    private fun hentForrigeVedtak(behandling: Saksbehandling): InnvilgelseEllerOpphørDagligReise? =
        behandling.forrigeIverksatteBehandlingId?.let { hentVedtak(it) }?.data
}
