package no.nav.tilleggsstonader.sak.behandlingsflyt.task

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgavePrioritet
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.libs.utils.osloNow
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OpprettOppgave
import no.nav.tilleggsstonader.sak.statistikk.task.BehandlingsstatistikkTask
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettOppgaveForOpprettetBehandlingTask.TYPE,
    beskrivelse = "Opprett behandle sak oppgave for opprettet behandling",
    maxAntallFeil = 3,
)
class OpprettOppgaveForOpprettetBehandlingTask(
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
    private val taskService: TaskService,
) : AsyncTaskStep {

    private val logger = LoggerFactory.getLogger(javaClass)

    data class OpprettOppgaveTaskData(
        val behandlingId: BehandlingId,
        val saksbehandler: String,
        val beskrivelse: String? = null,
        val hendelseTidspunkt: LocalDateTime = osloNow(),
        val prioritet: OppgavePrioritet = OppgavePrioritet.NORM,
    )

    override fun doTask(task: Task) {
        val data = objectMapper.readValue<OpprettOppgaveTaskData>(task.payload)
        val oppgaveId = opprettOppgave(data, task)

        taskService.save(
            BehandlingsstatistikkTask.opprettMottattTask(
                behandlingId = data.behandlingId,
                hendelseTidspunkt = data.hendelseTidspunkt,
                oppgaveId = oppgaveId,
            ),
        )
    }

    private fun opprettOppgave(
        data: OpprettOppgaveTaskData,
        task: Task,
    ): Long? {
        val behandling = behandlingService.hentSaksbehandling(data.behandlingId)
        if (behandling.status == BehandlingStatus.OPPRETTET || behandling.status == BehandlingStatus.UTREDES) {
            val tilordnetNavIdent =
                if (data.saksbehandler == SikkerhetContext.SYSTEM_FORKORTELSE) null else data.saksbehandler
            val oppgaveId = oppgaveService.opprettOppgave(
                behandlingId = data.behandlingId,
                oppgave = OpprettOppgave(
                    oppgavetype = Oppgavetype.BehandleSak,
                    tilordnetNavIdent = tilordnetNavIdent,
                    beskrivelse = data.beskrivelse,
                    prioritet = data.prioritet,
                ),
            )
            task.metadata.setProperty("oppgaveId", oppgaveId.toString())
            return oppgaveId
        } else {
            logger.warn("Oppretter ikke oppgave på behandling=${behandling.id} då den har status=${behandling.status}")
            return null
        }
    }

    companion object {

        fun opprettTask(data: OpprettOppgaveTaskData): Task {
            return Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(data),
                properties = Properties().apply {
                    this["saksbehandler"] = data.saksbehandler
                    this["behandlingId"] = data.behandlingId.toString()
                },
            )
        }

        const val TYPE = "opprettOppgaveForOpprettetBehandling"
    }
}
