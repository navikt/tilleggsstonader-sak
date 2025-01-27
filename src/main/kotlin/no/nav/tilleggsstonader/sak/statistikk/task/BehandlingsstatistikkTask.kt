package no.nav.tilleggsstonader.sak.statistikk.task

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.libs.utils.osloNow
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.statistikk.behandling.BehandlingsstatistikkService
import no.nav.tilleggsstonader.sak.statistikk.behandling.dto.BehandlingMetode
import no.nav.tilleggsstonader.sak.statistikk.behandling.dto.Hendelse
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = BehandlingsstatistikkTask.TYPE,
    beskrivelse = "Sender behandlingsstatistikk til DVH",
)
class BehandlingsstatistikkTask(
    private val behandlingsstatistikkService: BehandlingsstatistikkService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val (behandlingId, hendelse, hendelseTidspunkt, gjeldendeSaksbehandler, oppgaveId, behandlingMetode) =
            objectMapper.readValue<BehandlingsstatistikkTaskPayload>(task.payload)

        behandlingsstatistikkService.sendBehandlingstatistikk(
            behandlingId,
            hendelse,
            hendelseTidspunkt,
            gjeldendeSaksbehandler,
            oppgaveId,
            behandlingMetode,
        )
    }

    companion object {
        fun opprettMottattTask(
            behandlingId: BehandlingId,
            hendelseTidspunkt: LocalDateTime = osloNow(),
            oppgaveId: Long?,
        ): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = Hendelse.MOTTATT,
                hendelseTidspunkt = hendelseTidspunkt,
                oppgaveId = oppgaveId,
            )

        fun opprettPåbegyntTask(behandlingId: BehandlingId): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = Hendelse.PÅBEGYNT,
                hendelseTidspunkt = osloNow(),
            )

        fun opprettVenterTask(behandlingId: BehandlingId): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = Hendelse.VENTER,
                hendelseTidspunkt = osloNow(),
            )

        fun opprettVedtattTask(behandlingId: BehandlingId): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = Hendelse.VEDTATT,
                hendelseTidspunkt = osloNow(),
            )

        fun opprettBesluttetTask(behandlingId: BehandlingId): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = Hendelse.BESLUTTET,
                hendelseTidspunkt = osloNow(),
            )

        fun opprettFerdigTask(behandlingId: BehandlingId): Task =
            opprettTask(
                behandlingId = behandlingId,
                hendelse = Hendelse.FERDIG,
                hendelseTidspunkt = osloNow(),
            )

        private fun opprettTask(
            behandlingId: BehandlingId,
            hendelse: Hendelse,
            oppgaveId: Long? = null,
            hendelseTidspunkt: LocalDateTime = osloNow(),
            gjeldendeSaksbehandler: String? = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
            behandlingMetode: BehandlingMetode? = BehandlingMetode.MANUELL,
        ): Task =
            Task(
                type = TYPE,
                payload =
                    objectMapper.writeValueAsString(
                        BehandlingsstatistikkTaskPayload(
                            behandlingId,
                            hendelse,
                            hendelseTidspunkt,
                            gjeldendeSaksbehandler,
                            oppgaveId,
                            behandlingMetode,
                        ),
                    ),
                properties =
                    Properties().apply {
                        this["saksbehandler"] = gjeldendeSaksbehandler ?: ""
                        this["behandlingId"] = behandlingId.toString()
                        this["hendelse"] = hendelse.name
                        this["hendelseTidspunkt"] = hendelseTidspunkt.toString()
                        this["oppgaveId"] = oppgaveId?.toString() ?: ""
                    },
            )

        const val TYPE = "behandlingsstatistikkTask"
    }

    data class BehandlingsstatistikkTaskPayload(
        val behandlingId: BehandlingId,
        val hendelse: Hendelse,
        val hendelseTidspunkt: LocalDateTime,
        val gjeldendeSaksbehandler: String?,
        val oppgaveId: Long?,
        val behandlingMetode: BehandlingMetode?,
    )
}
