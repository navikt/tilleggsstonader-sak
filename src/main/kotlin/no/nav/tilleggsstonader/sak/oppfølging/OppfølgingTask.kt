package no.nav.tilleggsstonader.sak.oppfølging

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
@TaskStepBeskrivelse(
    taskStepType = OppfølgingTask.TYPE,
    beskrivelse = "Oppretter oppfølging for behandling",
)
class OppfølgingTask(
    private val oppfølgingOpprettKontrollerService: OppfølgingOpprettKontrollerService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val data = objectMapper.readValue<OppfølgingTaskData>(task.payload)
        oppfølgingOpprettKontrollerService.opprettOppfølging(data.behandlingId)
    }

    companion object {
        const val TYPE = "oppfølging"

        fun opprettTask(
            behandlingId: BehandlingId,
            tidspunkt: LocalDateTime,
        ) = Task(
            type = TYPE,
            payload = objectMapper.writeValueAsString(OppfølgingTaskData(behandlingId = behandlingId, tidspunkt)),
        )
    }
}

/**
 * @param tidspunkt er for å få en unik payload då payload skal være unik
 */
private data class OppfølgingTaskData(
    val behandlingId: BehandlingId,
    val tidspunkt: LocalDateTime,
)
