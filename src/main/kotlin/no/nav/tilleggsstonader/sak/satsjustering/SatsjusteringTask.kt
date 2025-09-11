package no.nav.tilleggsstonader.sak.satsjustering

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = SatsjusteringTask.TYPE,
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
    beskrivelse = "Oppretter revurdering og iverksetter vedtak med satsjustering",
)
class SatsjusteringTask(
    private val utførSatsjusteringService: UtførSatsjusteringService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        utførSatsjusteringService.kjørSatsjustering(behandlingId = BehandlingId.fromString(task.payload))
    }

    companion object {
        const val TYPE = "satsjusteringTask"

        fun opprettTask(behandlingId: BehandlingId) =
            Task(
                type = TYPE,
                payload = behandlingId.toString(),
            )
    }
}
