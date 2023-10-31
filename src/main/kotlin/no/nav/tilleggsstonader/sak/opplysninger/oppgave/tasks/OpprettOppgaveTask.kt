package no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettOppgaveTask.TYPE,
    beskrivelse = "Opprett oppgave i GOSYS for behandling",
    maxAntallFeil = 3,
)
class OpprettOppgaveTask(
    private val oppgaveService: OppgaveService,
    private val behandlingService: BehandlingService,
) : AsyncTaskStep {

    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Då payload er unik per task type, så settes unik inn
     */
    data class OpprettOppgaveTaskData(
        val behandlingId: UUID,
        val oppgavetype: Oppgavetype,
        val tilordnetNavIdent: String? = null,
        val beskrivelse: String? = null,
        val unik: LocalDateTime? = LocalDateTime.now(),
    )

    override fun doTask(task: Task) {
        val data = objectMapper.readValue<OpprettOppgaveTaskData>(task.payload)
        val oppgavetype = data.oppgavetype

        if (oppgavetype == Oppgavetype.BehandleSak) {
            val behandling = behandlingService.hentBehandling(data.behandlingId)

            /**
             * Nødvendig sjekk for å unngå å lage en behandle sak oppgave som aldri ferdigstilles dersom
             * SB sender til beslutter rett etter angring og oppgave ikke er opprettet enda.
             */
            if (behandling.status.behandlingErLåstForVidereRedigering()) {
                logger.info("Opprettet ikke oppgave med oppgavetype = $oppgavetype fordi status = ${behandling.status}")
                return
            }
        }

        val oppgaveId = oppgaveService.opprettOppgave(
            behandlingId = data.behandlingId,
            oppgavetype = oppgavetype,
            tilordnetNavIdent = data.tilordnetNavIdent,
            beskrivelse = data.beskrivelse,
        )
        logger.info("Opprettet oppgave=$oppgaveId for oppgavetype=$oppgavetype")

        task.metadata.setProperty("oppgaveId", oppgaveId.toString())
    }

    companion object {

        fun opprettTask(data: OpprettOppgaveTaskData): Task {
            return Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(data),
                properties = Properties().apply {
                    setProperty("saksbehandler", SikkerhetContext.hentSaksbehandlerEllerSystembruker())
                    setProperty("behandlingId", data.behandlingId.toString())
                    setProperty("oppgavetype", data.oppgavetype.name)
                },
            )
        }

        const val TYPE = "opprettOppgave"
    }
}
