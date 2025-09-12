package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.MålgruppeValidering.validerNyeMålgrupperOverlapperIkkeMedEksisterendeMålgrupper
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.springframework.stereotype.Service

@Service
class InngangsvilkårSteg(
    private val behandlingService: BehandlingService,
    private val vilkårperiodeService: VilkårperiodeService,
) : BehandlingSteg<Void?> {
    override fun validerSteg(saksbehandling: Saksbehandling) {
        val vilkårperioder = vilkårperiodeService.hentVilkårperioder(saksbehandling.id)

        brukerfeilHvis((vilkårperioder.målgrupper.isEmpty() || vilkårperioder.aktiviteter.isEmpty())) {
            "Du må registrere minst én målgruppe og én aktivitet for å kunne gå videre."
        }

        validerNyeMålgrupperOverlapperIkkeMedEksisterendeMålgrupper(vilkårperioder)
    }

    override fun utførSteg(
        saksbehandling: Saksbehandling,
        data: Void?,
    ) {
        behandlingService.markerBehandlingSomPåbegyntHvisDenHarStatusOpprettet(
            behandlingId = saksbehandling.id,
            saksbehandling.status,
            saksbehandling.steg,
        )
    }

    override fun stegType(): StegType = StegType.INNGANGSVILKÅR
}
