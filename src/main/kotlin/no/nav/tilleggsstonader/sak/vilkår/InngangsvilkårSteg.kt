package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import org.springframework.stereotype.Service

@Service
class InngangsvilkårSteg(
    private val behandlingService: BehandlingService,
) : BehandlingSteg<Void?> {
    override fun validerSteg(saksbehandling: Saksbehandling) {
        brukerfeilHvis(saksbehandling.type == BehandlingType.REVURDERING && saksbehandling.revurderFra == null) {
            "Du må sette revurder fra-dato før du kan gå videre"
        }
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
