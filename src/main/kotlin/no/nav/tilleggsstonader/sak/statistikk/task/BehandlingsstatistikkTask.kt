package no.nav.tilleggsstonader.sak.statistikk.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.error.RekjørSenereException
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingMetode
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import no.nav.tilleggsstonader.sak.statistikk.behandling.BehandlingsstatistikkService
import no.nav.tilleggsstonader.sak.statistikk.behandling.Hendelse
import org.springframework.stereotype.Service
import tools.jackson.module.kotlin.readValue
import java.time.LocalDateTime
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = BehandlingsstatistikkTask.TYPE,
    beskrivelse = "Sender behandlingsstatistikk til DVH",
)
class BehandlingsstatistikkTask(
    private val behandlingsstatistikkService: BehandlingsstatistikkService,
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val (behandlingId, hendelse, hendelseTidspunkt, gjeldendeSaksbehandler) =
            jsonMapper.readValue<BehandlingsstatistikkTaskPayload>(task.payload)

        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)

        if (saksbehandling.behandlingMetode == BehandlingMetode.MANUELL) {
            kastFeilOmOppgaveIkkeHarBlittOpprettet(saksbehandling)
        }

        behandlingsstatistikkService.sendBehandlingstatistikk(
            saksbehandling,
            hendelse,
            hendelseTidspunkt,
            gjeldendeSaksbehandler,
        )
    }

    // Vi sender med ansvarligEnhet til DVH, som hentes ut fra oppgave. Venter på at oppgave skal opprettes
    private fun kastFeilOmOppgaveIkkeHarBlittOpprettet(saksbehandling: Saksbehandling) {
        val oppgaverForBehandling = oppgaveService.finnAlleOppgaveDomainForBehandling(saksbehandling.id)
        if (oppgaverForBehandling.isEmpty()) {
            throw RekjørSenereException(
                "Vent med å sende MOTTATT-status til DVH til oppgave er opprettet for behandling=${saksbehandling.id}",
                triggerTid = LocalDateTime.now().plusSeconds(5),
            )
        }
    }

    companion object {
        fun opprettMottattTask(
            behandlingId: BehandlingId,
            hendelseTidspunkt: LocalDateTime,
        ): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = Hendelse.MOTTATT,
                hendelseTidspunkt = hendelseTidspunkt,
            )

        fun opprettPåbegyntTask(behandlingId: BehandlingId): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = Hendelse.PÅBEGYNT,
                hendelseTidspunkt = LocalDateTime.now(),
            )

        fun opprettVenterTask(behandlingId: BehandlingId): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = Hendelse.VENTER,
                hendelseTidspunkt = LocalDateTime.now(),
            )

        fun opprettVedtattTask(behandlingId: BehandlingId): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = Hendelse.VEDTATT,
                hendelseTidspunkt = LocalDateTime.now(),
            )

        fun opprettAngretSendtTilBeslutterTask(behandlingId: BehandlingId): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = Hendelse.ANGRET_SENDT_TIL_BESLUTTER,
                hendelseTidspunkt = LocalDateTime.now(),
            )

        fun opprettUnderkjentBeslutterTask(behandlingId: BehandlingId): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = Hendelse.UNDERKJENT_BESLUTTER,
                hendelseTidspunkt = LocalDateTime.now(),
            )

        fun opprettBesluttetTask(behandlingId: BehandlingId): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = Hendelse.BESLUTTET,
                hendelseTidspunkt = LocalDateTime.now(),
            )

        fun opprettFerdigTask(behandlingId: BehandlingId): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = Hendelse.FERDIG,
                hendelseTidspunkt = LocalDateTime.now(),
            )

        private fun opprettTask(
            behandlingId: BehandlingId,
            hendelse: Hendelse,
            hendelseTidspunkt: LocalDateTime = LocalDateTime.now(),
            gjeldendeSaksbehandler: String? = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
        ): Task =
            Task(
                type = TYPE,
                payload =
                    jsonMapper.writeValueAsString(
                        BehandlingsstatistikkTaskPayload(
                            behandlingId,
                            hendelse,
                            hendelseTidspunkt,
                            gjeldendeSaksbehandler,
                        ),
                    ),
                properties =
                    Properties().apply {
                        this["saksbehandler"] = gjeldendeSaksbehandler ?: ""
                        this["behandlingId"] = behandlingId.toString()
                        this["hendelse"] = hendelse.name
                        this["hendelseTidspunkt"] = hendelseTidspunkt.toString()
                    },
            )

        const val TYPE = "behandlingsstatistikkTask"
    }

    data class BehandlingsstatistikkTaskPayload(
        val behandlingId: BehandlingId,
        val hendelse: Hendelse,
        val hendelseTidspunkt: LocalDateTime,
        val gjeldendeSaksbehandler: String?,
    )
}
