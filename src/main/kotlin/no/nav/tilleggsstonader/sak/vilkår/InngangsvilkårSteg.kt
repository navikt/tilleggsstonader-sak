package no.nav.tilleggsstonader.sak.vilkår

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import org.springframework.stereotype.Service

@Service
class InngangsvilkårSteg(
    private val behandlingService: BehandlingService,
) : BehandlingSteg<Void?> {

    override fun utførSteg(saksbehandling: Saksbehandling, data: Void?) {
        if (saksbehandling.status != BehandlingStatus.UTREDES) {
            behandlingService.oppdaterStatusPåBehandling(saksbehandling.id, BehandlingStatus.UTREDES)
        }
    }

    /**
     * håndteres av [StønadsperiodeService]
     */
    override fun settInnHistorikk() = false

    override fun stegType(): StegType = StegType.INNGANGSVILKÅR
}
