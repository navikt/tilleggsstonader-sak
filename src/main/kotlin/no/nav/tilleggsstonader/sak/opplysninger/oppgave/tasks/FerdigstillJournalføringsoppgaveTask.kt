package no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import org.springframework.stereotype.Service
import java.util.*

@Service
@TaskStepBeskrivelse(
    taskStepType = FerdigstillJournalføringsoppgaveTask.TYPE,
    beskrivelse = "Avslutt journalføringsoppgave oppgave i oppgave systemet",
    maxAntallFeil = 3,
)
class FerdigstillJournalføringsoppgaveTask(private val oppgaveService: OppgaveService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val oppgaveId = task.payload
        oppgaveService.ferdigstillOppgave(oppgaveId.toLong())
    }

    companion object {

        fun opprettTask(oppgaveId: String): Task {
            return Task(
                type = TYPE,
                payload = oppgaveId,
                properties = Properties().apply {
                    setProperty("saksbehandler", SikkerhetContext.hentSaksbehandlerEllerSystembruker())
                    setProperty("oppgaveId", oppgaveId)
                },
            )
        }

        const val TYPE = "ferdigstillJournalføringsoppgave"
    }
}
