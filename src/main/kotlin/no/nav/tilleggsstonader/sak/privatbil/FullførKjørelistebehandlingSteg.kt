package no.nav.tilleggsstonader.sak.privatbil

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakereService
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import org.springframework.stereotype.Component

@Component
class FullførKjørelistebehandlingSteg(
    private val behandlingService: BehandlingService,
    private val fullførKjørelistebehandlingService: FullførKjørelistebehandlingService,
    private val brevmottakereService: BrevmottakereService,
) : BehandlingSteg<Void?> {
    override fun utførSteg(
        saksbehandling: Saksbehandling,
        data: Void?,
    ) {
        brukerfeilHvis(saksbehandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke fullføre kjørelistebehandling=${saksbehandling.id} fordi den har status ${saksbehandling.status.visningsnavn()}."
        }
        brukerfeilHvis(saksbehandling.type != BehandlingType.KJØRELISTE) {
            "Kan ikke fullføre behandling=${saksbehandling.id} fordi den ikke er en kjørelistebehandling."
        }

        // For å opprette brevmottakere for kjøreliste-brev da det ikke gjøres ved automatisk behandling
        brevmottakereService.hentEllerOpprettBrevmottakere(saksbehandling.id)
        fullførKjørelistebehandlingService.fullførKjørelistebehandling(saksbehandling)
        behandlingService.oppdaterStatusPåBehandling(saksbehandling.id, BehandlingStatus.IVERKSETTER_VEDTAK)
    }

    override fun stegType() = StegType.FULLFØR_KJØRELISTE
}
