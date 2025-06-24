package no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveUtil.skalIkkeOppretteOppgave
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OpprettOppgave
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettOppgaveTask.TYPE,
    beskrivelse = "Opprett oppgave i GOSYS for behandling",
    maxAntallFeil = 3,
)
class OpprettOppgaveTask(
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Då payload er unik per task type, så settes unik inn
     */
    data class OpprettOppgaveTaskData(
        val kobling: Oppgavekobling,
        val oppgave: OpprettOppgave,
        val unik: LocalDateTime? = LocalDateTime.now(),
    )

    override fun doTask(task: Task) {
        val data = objectMapper.readValue<OpprettOppgaveTaskData>(task.payload)
        val kobling = data.kobling
        val oppgavetype = data.oppgave.oppgavetype

        val koblingPerson: OppgavekoblingPerson =
            when (kobling) {
                is OppgavekoblingBehandling -> {
                    val behandling = behandlingService.hentSaksbehandling(kobling.behandlingId)
                    if (skalIkkeOppretteOppgave(behandling, oppgavetype)) {
                        logger.warn("Opprettet ikke oppgave med oppgavetype=$oppgavetype fordi status=${behandling.status}")
                        return
                    }
                    OppgavekoblingPerson(behandling.ident, behandling.stønadstype)
                }

                is OppgavekoblingPerson -> kobling
            }

        val oppgaveId =
            oppgaveService.opprettOppgave(
                personIdent = koblingPerson.personIdent,
                stønadstype = koblingPerson.stønadstype,
                behandlingId = kobling.let { if (it is OppgavekoblingBehandling) it else null }?.behandlingId,
                oppgave = data.oppgave,
            )
        logger.info("Opprettet oppgave=$oppgaveId for oppgavetype=$oppgavetype")

        task.metadata.setProperty("oppgaveId", oppgaveId.toString())
    }

    companion object {
        fun opprettTask(
            behandlingId: BehandlingId,
            oppgave: OpprettOppgave,
        ): Task = opprettTask(OppgavekoblingBehandling(behandlingId), oppgave)

        fun opprettTask(
            personIdent: String,
            stønadstype: Stønadstype,
            oppgave: OpprettOppgave,
        ): Task = opprettTask(OppgavekoblingPerson(personIdent, stønadstype), oppgave)

        private fun opprettTask(
            oppgavekobling: Oppgavekobling,
            oppgave: OpprettOppgave,
        ): Task =
            Task(
                type = TYPE,
                payload =
                    objectMapper.writeValueAsString(
                        OpprettOppgaveTaskData(
                            kobling = oppgavekobling,
                            oppgave = oppgave,
                        ),
                    ),
                properties =
                    Properties().apply {
                        setProperty("saksbehandler", SikkerhetContext.hentSaksbehandlerEllerSystembruker())
                        setProperty("oppgavetype", oppgave.oppgavetype.name)
                        if (oppgavekobling is OppgavekoblingBehandling) {
                            setProperty("behandlingId", oppgavekobling.behandlingId.toString())
                        }
                    },
            )

        const val TYPE = "opprettOppgave"
    }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes(
    JsonSubTypes.Type(OppgavekoblingBehandling::class, name = "OpprettOppgave"),
    JsonSubTypes.Type(OppgavekoblingPerson::class, name = "OpprettOppgaveForBehandling"),
)
sealed class Oppgavekobling

data class OppgavekoblingBehandling(
    val behandlingId: BehandlingId,
) : Oppgavekobling()

data class OppgavekoblingPerson(
    val personIdent: String,
    val stønadstype: Stønadstype,
) : Oppgavekobling()
