package no.nav.tilleggsstonader.sak.privatbil

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.FerdigstillBehandlingTask
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakereService
import no.nav.tilleggsstonader.sak.brev.kjørelistebrev.JournalførKjørelisteBehandlingBrevTask
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettService
import org.springframework.stereotype.Service

@Service
class FullførKjørelistebehandlingService(
    private val taskService: TaskService,
    private val iverksettService: IverksettService,
    private val behandlingService: BehandlingService,
    private val brevmottakereService: BrevmottakereService,
) {
    fun fullførKjørelistebehandling(saksbehandling: Saksbehandling) {
        brukerfeilHvis(saksbehandling.status == BehandlingStatus.FERDIGSTILT) {
            "Kan ikke fullføre kjørelistebehandling=${saksbehandling.id} fordi den har status ${saksbehandling.status.visningsnavn()}."
        }
        brukerfeilHvis(saksbehandling.type != BehandlingType.KJØRELISTE) {
            "Kan ikke fullføre behandling=${saksbehandling.id} fordi den ikke er en kjørelistebehandling."
        }

        opprettTaskForSendingAvVedtaksbrev(saksbehandling.id)
        behandlingService.oppdaterResultatPåBehandling(saksbehandling.id, BehandlingResultat.INNVILGET)

        taskService.save(FerdigstillBehandlingTask.opprettTask(saksbehandling))
        iverksettService.iverksettBehandlingFørsteGang(saksbehandling.id)
    }

    private fun opprettTaskForSendingAvVedtaksbrev(behandlingId: BehandlingId) {
        // For å opprette brevmottakere for kjøreliste-brev da det ikke gjøres fra frontend eller ved automatisk behandling
        brevmottakereService.hentEllerOpprettBrevmottakere(behandlingId)
        taskService.save(JournalførKjørelisteBehandlingBrevTask.opprettTask(behandlingId))
    }
}
