package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = HentStatusFraIverksettingTask.TYPE,
    maxAntallFeil = 50,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 31L,
    beskrivelse = "Henter status på utbetaling av behandling.",
)
class HentStatusFraIverksettingTask(
    private val iverksettService: IverksettService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val taskData = objectMapper.readValue<TaskData>(task.payload)
        iverksettService.hentStatusOgOppdaterAndeler(
            behandlingId = taskData.behandlingId,
            iverksettingId = taskData.iverksettingId,
        )
    }

    companion object {

        fun opprettTask(behandlingId: UUID, iverksettingId: UUID): Task {
            val taskData = TaskData(behandlingId = behandlingId, iverksettingId = iverksettingId)
            return Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(taskData),
                properties = Properties().apply {
                    setProperty("behandlingId", behandlingId.toString())
                    setProperty("iverksettingId", iverksettingId.toString())
                },
            ).copy(triggerTid = LocalDateTime.now().plusSeconds(31))
        }

        const val TYPE = "statusFraIverksetting"
    }

    private data class TaskData(
        val behandlingId: UUID,
        val iverksettingId: UUID,
    )
}
