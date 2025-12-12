package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatDagligReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.VilkårDagligReiseMapper.mapTilVilkårDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
import org.springframework.stereotype.Service

@Service
class DagligReiseBeregningService(
    private val vilkårService: VilkårService,
    private val vedtaksperiodeValideringService: VedtaksperiodeValideringService,
    private val offentligTransportBeregningService: OffentligTransportBeregningService,
    private val dagligReiseBeregningRevurderingService: DagligReiseBeregningRevurderingService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
) {
    fun beregn(
        vedtaksperioder: List<Vedtaksperiode>,
        behandling: Saksbehandling,
        typeVedtak: TypeVedtak,
    ): BeregningsresultatDagligReise {
        vedtaksperiodeValideringService.validerVedtaksperioder(
            vedtaksperioder = vedtaksperioder,
            behandling = behandling,
            typeVedtak = typeVedtak,
        )

        val oppfylteVilkårDagligReise = vilkårService.hentOppfylteDagligReiseVilkår(behandling.id).map { it.mapTilVilkårDagligReise() }
        validerFinnesReiser(oppfylteVilkårDagligReise)

        val brukersNavKontor =
            if (behandling.stønadstype == Stønadstype.DAGLIG_REISE_TSR) {
                arbeidsfordelingService.hentBrukersNavKontor(behandling.ident, behandling.stønadstype)
            } else {
                null
            }

        val beregningsresultatOffentligTransport =
            beregnOffentligTransport(
                oppfylteVilkårDagligReise = oppfylteVilkårDagligReise,
                vedtaksperioder = vedtaksperioder,
                behandling = behandling,
                brukersNavKontor = brukersNavKontor,
            )

        return BeregningsresultatDagligReise(
            offentligTransport = beregningsresultatOffentligTransport,
        )
    }

    private fun beregnOffentligTransport(
        oppfylteVilkårDagligReise: List<VilkårDagligReise>,
        vedtaksperioder: List<Vedtaksperiode>,
        behandling: Saksbehandling,
        brukersNavKontor: String?,
    ): BeregningsresultatOffentligTransport? {
        val oppfylteVilkårOffentligTransport = oppfylteVilkårDagligReise.filter { it.fakta is FaktaOffentligTransport }

        if (oppfylteVilkårOffentligTransport.isEmpty()) return null

        return offentligTransportBeregningService
            .beregn(
                vedtaksperioder = vedtaksperioder,
                oppfylteVilkår = oppfylteVilkårOffentligTransport,
                brukersNavKontor = brukersNavKontor,
            ).let {
                dagligReiseBeregningRevurderingService.flettMedForrigeVedtakHvisRevurdering(
                    behandling = behandling,
                    vedtaksperioder = vedtaksperioder,
                    nyttBeregningsresultat = it,
                )
            }
    }

    private fun validerFinnesReiser(vilkår: List<VilkårDagligReise>) {
        brukerfeilHvis(vilkår.isEmpty()) {
            "Innvilgelse er ikke et gyldig vedtaksresultat når det ikke er lagt inn perioder med reise"
        }
    }
}
