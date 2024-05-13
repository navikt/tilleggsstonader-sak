package no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.libs.utils.osloNow
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = FerdigstillOppgaveTask.TYPE,
    beskrivelse = "Avslutt oppgave i GOSYS",
    maxAntallFeil = 3,
)
class FerdigstillOppgaveTask(private val oppgaveService: OppgaveService) : AsyncTaskStep {

    /**
     * Då payload er unik per task type, så settes unik inn
     */
    data class FerdigstillOppgaveTaskData(
        val behandlingId: UUID,
        val oppgavetype: Oppgavetype,
        val unik: LocalDateTime? = osloNow(),
    )

    override fun doTask(task: Task) {
        val data = objectMapper.readValue<FerdigstillOppgaveTaskData>(task.payload)
        oppgaveService.ferdigstillBehandleOppgave(
            behandlingId = data.behandlingId,
            oppgavetype = data.oppgavetype,
        )
    }

    companion object {

        fun opprettTask(behandlingId: UUID, oppgavetype: Oppgavetype, oppgaveId: Long?): Task {
            return Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(FerdigstillOppgaveTaskData(behandlingId, oppgavetype)),
                properties = Properties().apply {
                    setProperty("saksbehandler", SikkerhetContext.hentSaksbehandlerEllerSystembruker())
                    setProperty("behandlingId", behandlingId.toString())
                    setProperty("oppgavetype", oppgavetype.name)
                    setProperty("oppgaveId", oppgaveId.toString())
                },
            )
        }

        const val TYPE = "ferdigstillOppgave"
    }
}
