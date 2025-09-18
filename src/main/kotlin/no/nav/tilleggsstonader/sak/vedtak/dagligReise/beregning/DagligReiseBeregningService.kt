package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.Beregningsresultat
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import org.springframework.stereotype.Service

@Service
class DagligReiseBeregningService(
    private val vilkårService: VilkårService,
    private val vedtaksperiodeValideringService: VedtaksperiodeValideringService,
    private val offentligTransportBeregningService: OffentligTransportBeregningService,
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

        val oppfylteVilkår = vilkårService.hentOppfylteDagligReiseVilkår(behandlingId)
        validerUtgifter(oppfylteVilkår)

        val oppfylteVilkårGruppertPåType = oppfylteVilkår.groupBy { it.type }

        return BeregningsresultatDagligReise(
            offentligTransport = beregnOffentligTransport(oppfylteVilkårGruppertPåType, vedtaksperioder),
        )
    }

    private fun beregnOffentligTransport(
        vilkår: Map<VilkårType, List<Vilkår>>,
        vedtaksperioder: List<Vedtaksperiode>,
    ): Beregningsresultat? {
        val vilkårOffentligTransport = vilkår[VilkårType.DAGLIG_REISE_OFFENTLIG_TRANSPORT] ?: return null

        return offentligTransportBeregningService.beregn(
            vedtaksperioder = vedtaksperioder,
            oppfylteVilkår = vilkårOffentligTransport,
        )
    }

    private fun validerUtgifter(vilkår: List<Vilkår>) {
        brukerfeilHvis(vilkår.isEmpty()) {
            "Innvilgelse er ikke et gyldig vedtaksresultat når det ikke er lagt inn perioder med reise"
        }
    }
}
