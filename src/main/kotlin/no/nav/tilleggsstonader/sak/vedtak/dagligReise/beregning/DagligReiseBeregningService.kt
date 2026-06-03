package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.offentligTransport.OffentligTransportBeregningService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil.PrivatBilBeregningService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil.PrivatBilRammevedtakBeregningService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatDagligReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.VilkårDagligReiseMapper.mapTilVilkårDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
import org.springframework.stereotype.Service

@Service
class DagligReiseBeregningService(
    private val vilkårService: VilkårService,
    private val dagligReiseVedtaksperioderValideringService: DagligReiseVedtaksperioderValideringService,
    private val offentligTransportBeregningService: OffentligTransportBeregningService,
    private val privatBilRammevedtakBeregningService: PrivatBilRammevedtakBeregningService,
    private val privatBilBeregningService: PrivatBilBeregningService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val vedtakService: VedtakService,
) {
    fun beregn(
        vedtaksperioder: List<Vedtaksperiode>,
        behandling: Saksbehandling,
        beregningsplan: Beregningsplan,
        typeVedtak: TypeVedtak,
    ): BeregningDagligReise {
        dagligReiseVedtaksperioderValideringService.validerVedtaksperioder(
            vedtaksperioder = vedtaksperioder,
            behandling = behandling,
            typeVedtak = typeVedtak,
        )

        val oppfylteVilkårDagligReise =
            vilkårService.hentOppfylteDagligReiseVilkår(behandling.id).map { it.mapTilVilkårDagligReise() }

        validerFinnesReiser(oppfylteVilkårDagligReise)

        val brukersNavKontor =
            if (behandling.stønadstype == Stønadstype.DAGLIG_REISE_TSR) {
                arbeidsfordelingService.hentBrukersNavKontor(behandling.ident, behandling.stønadstype)
            } else {
                null
            }

        val forrigeVedtak =
            behandling.forrigeIverksatteBehandlingId
                ?.let { vedtakService.hentVedtak<InnvilgelseEllerOpphørDagligReise>(it).data }

        val offentligTransport =
            offentligTransportBeregningService.beregn(
                vedtaksperioder = vedtaksperioder,
                oppfylteVilkårDagligReise = oppfylteVilkårDagligReise,
                forrigeBeregningsresultat = forrigeVedtak?.beregningsresultat?.offentligTransport,
                brukersNavKontor = brukersNavKontor,
                beregningsplan = beregningsplan,
                behandling = behandling,
            )

        val rammevedtakPrivatBil =
            privatBilRammevedtakBeregningService.beregnRammevedtak(
                vedtaksperioder = vedtaksperioder,
                oppfylteVilkårDagligReise = oppfylteVilkårDagligReise,
                behandlingId = behandling.id,
            )

        val beregningsresultatPrivatBil =
            privatBilBeregningService.beregn(
                behandling = behandling,
                rammevedtak = rammevedtakPrivatBil,
                brukersNavKontor = brukersNavKontor,
                forrigeBeregningsresultat = forrigeVedtak?.beregningsresultat?.privatBil,
            )

        return BeregningDagligReise(
            beregningsresultatDagligReise =
                BeregningsresultatDagligReise(
                    offentligTransport = offentligTransport,
                    privatBil = beregningsresultatPrivatBil,
                ),
            rammevedtakPrivatBil = rammevedtakPrivatBil,
        )
    }
}

private fun validerFinnesReiser(vilkår: List<VilkårDagligReise>) {
    brukerfeilHvis(vilkår.isEmpty()) {
        "Innvilgelse er ikke et gyldig vedtaksresultat når det ikke er lagt inn perioder med reise"
    }
}

data class BeregningDagligReise(
    val beregningsresultatDagligReise: BeregningsresultatDagligReise,
    val rammevedtakPrivatBil: RammevedtakPrivatBil?,
)
