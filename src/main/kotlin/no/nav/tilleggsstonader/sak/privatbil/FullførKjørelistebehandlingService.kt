package no.nav.tilleggsstonader.sak.privatbil

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.FerdigstillBehandlingTask
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakereService
import no.nav.tilleggsstonader.sak.brev.kjørelistebrev.JournalførKjørelisteBehandlingBrevTask
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.FerdigstillOppgaveTask
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettService
import org.springframework.stereotype.Service

@Service
class FullførKjørelistebehandlingService(
    private val taskService: TaskService,
    private val iverksettService: IverksettService,
    private val brevmottakereService: BrevmottakereService,
    private val oppgaveService: OppgaveService,
) {
    fun fullførKjørelistebehandling(saksbehandling: Saksbehandling) {
        brukerfeilHvis(saksbehandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke fullføre kjørelistebehandling=${saksbehandling.id} fordi den har status ${saksbehandling.status.visningsnavn()}."
        }
        brukerfeilHvis(saksbehandling.type != BehandlingType.KJØRELISTE) {
            "Kan ikke fullføre behandling=${saksbehandling.id} fordi den ikke er en kjørelistebehandling."
        }

        opprettTaskForSendingAvVedtaksbrev(saksbehandling.id)
        taskService.save(FerdigstillBehandlingTask.opprettTask(saksbehandling))
        ferdigstillOppgave(saksbehandling)
        iverksettService.iverksettBehandlingFørsteGang(saksbehandling.id)
    }

    private fun opprettTaskForSendingAvVedtaksbrev(behandlingId: BehandlingId) {
        // For å opprette brevmottakere for kjøreliste-brev da det ikke gjøres fra frontend eller ved automatisk behandling
        brevmottakereService.hentEllerOpprettBrevmottakere(behandlingId)
        taskService.save(JournalførKjørelisteBehandlingBrevTask.opprettTask(behandlingId))
    }

    private fun ferdigstillOppgave(saksbehandling: Saksbehandling): Long? {
        val oppgavetype = Oppgavetype.BehandleKjøreliste
        return oppgaveService.hentOppgaveDomainSomIkkeErFerdigstilt(saksbehandling.id, oppgavetype)?.let {
            taskService.save(
                FerdigstillOppgaveTask.opprettTask(
                    behandlingId = saksbehandling.id,
                    oppgavetype = oppgavetype,
                    oppgaveId = it.gsakOppgaveId,
                ),
            )
            it.gsakOppgaveId
        }
    }
}
