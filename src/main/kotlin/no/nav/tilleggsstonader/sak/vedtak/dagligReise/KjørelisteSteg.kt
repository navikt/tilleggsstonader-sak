package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import org.springframework.stereotype.Service

@Service
class KjørelisteSteg(
    private val behandlingService: BehandlingService,
) : BehandlingSteg<Void?> {
    override fun utførSteg(
        saksbehandling: Saksbehandling,
        data: Void?,
    ) {
        behandlingService.markerBehandlingSomPåbegyntHvisDenHarStatusOpprettet(
            behandlingId = saksbehandling.id,
            behandlingSteg = saksbehandling.steg,
            behandlingStatus = saksbehandling.status,
        )
        // TODO: Beregn kjøreliste og lagre ned resultat
    }

    override fun stegType(): StegType = StegType.KJØRELISTE
}
