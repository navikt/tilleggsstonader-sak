package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.MålgruppeValidering.validerNyeMålgrupperOverlapperIkkeMedEksisterendeMålgrupper
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.springframework.stereotype.Service

@Service
class InngangsvilkårSteg(
    private val behandlingService: BehandlingService,
    private val vilkårperiodeService: VilkårperiodeService,
    private val unleashService: UnleashService,
) : BehandlingSteg<Void?> {
    override fun validerSteg(saksbehandling: Saksbehandling) {
        if (!unleashService.isEnabled(Toggle.SKAL_UTLEDE_ENDRINGSDATO_AUTOMATISK)) {
            brukerfeilHvis(saksbehandling.type == BehandlingType.REVURDERING && saksbehandling.revurderFra == null) {
                "Du må sette revurder fra-dato før du kan gå videre"
            }
        }
        val vilkårperioder = vilkårperiodeService.hentVilkårperioder(saksbehandling.id)
        validerNyeMålgrupperOverlapperIkkeMedEksisterendeMålgrupper(vilkårperioder)
    }

    override fun utførSteg(
        saksbehandling: Saksbehandling,
        data: Void?,
    ) {
        behandlingService.markerBehandlingSomPåbegynt(
            behandlingId = saksbehandling.id,
            saksbehandling.status,
            saksbehandling.steg,
        )
    }

    override fun stegType(): StegType = StegType.INNGANGSVILKÅR
}
