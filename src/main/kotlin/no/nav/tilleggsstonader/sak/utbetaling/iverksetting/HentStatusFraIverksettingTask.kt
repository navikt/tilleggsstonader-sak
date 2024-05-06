package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.libs.utils.osloNow
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = HentStatusFraIverksettingTask.TYPE,
    maxAntallFeil = 50,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 60L,
    beskrivelse = "Henter status på utbetaling av behandling.",
)
class HentStatusFraIverksettingTask(
    private val iverksettStatusService: IverksettStatusService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val taskData = objectMapper.readValue<TaskData>(task.payload)
        iverksettStatusService.hentStatusOgOppdaterAndeler(
            eksternFagsakId = taskData.eksternFagsakId,
            behandlingId = taskData.behandlingId,
            eksternBehandlingId = taskData.eksternBehandlingId,
            iverksettingId = taskData.iverksettingId,
        )
    }

    companion object {

        fun opprettTask(
            eksternFagsakId: Long,
            behandlingId: UUID,
            eksternBehandlingId: Long,
            iverksettingId: UUID,
        ): Task {
            val taskData = TaskData(
                eksternFagsakId = eksternFagsakId,
                behandlingId = behandlingId,
                eksternBehandlingId = eksternBehandlingId,
                iverksettingId = iverksettingId,
            )
            return Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(taskData),
                properties = Properties().apply {
                    setProperty("eksternFagsakId", eksternFagsakId.toString())
                    setProperty("behandlingId", behandlingId.toString())
                    setProperty("eksternBehandlingId", eksternBehandlingId.toString())
                    setProperty("iverksettingId", iverksettingId.toString())
                },
            ).copy(triggerTid = osloNow().plusSeconds(31))
        }

        const val TYPE = "statusFraIverksetting"
    }

    private data class TaskData(
        val eksternFagsakId: Long,
        val behandlingId: UUID,
        val eksternBehandlingId: Long,
        val iverksettingId: UUID,
    )
}
