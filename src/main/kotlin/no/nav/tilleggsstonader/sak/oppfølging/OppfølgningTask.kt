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
    taskStepType = OppfølgningTask.TYPE,
    beskrivelse = "Oppretter oppfølgning for behandling",
)
class OppfølgningTask(
    private val oppfølgingService: OppfølgingService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val data = objectMapper.readValue<OppfølgningTaskData>(task.payload)
        oppfølgingService.håndterBehandling(data.behandlingId)
    }

    companion object {
        const val TYPE = "oppfølgning"

        fun opprettTask(
            behandlingId: BehandlingId,
            tidspunkt: LocalDateTime,
        ) = Task(
            type = TYPE,
            payload = objectMapper.writeValueAsString(OppfølgningTaskData(behandlingId = behandlingId, tidspunkt)),
        )
    }
}

/**
 * @param tidspunkt er for å få en unik payload då payload skal være unik
 */
private data class OppfølgningTaskData(
    val behandlingId: BehandlingId,
    val tidspunkt: LocalDateTime,
)
