package no.nav.tilleggsstonader.sak.utbetaling.id

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = FagsakUtbetalingIdMigrieringTask.TYPE,
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
    beskrivelse = "Migrer en fagsak for utbetaling gjennom Kafka",
)
class FagsakUtbetalingIdMigrieringTask(
    val fagsakUtbetalingIdMigreringService: FagsakUtbetalingIdMigreringService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val bleMigrert = fagsakUtbetalingIdMigreringService.migrerForFagsak(FagsakId.fromString(task.payload))
        feilHvisIkke(bleMigrert) {
            "Fagsak ${task.payload} ble ikke migrert"
        }
    }

    companion object {
        fun opprettTask(fagsakId: FagsakId): Task =
            Task(
                type = TYPE,
                payload = fagsakId.toString(),
            )

        const val TYPE = "fagsakUtbetalingIdMigrieringTask "
    }
}
