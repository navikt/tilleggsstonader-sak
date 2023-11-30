package no.nav.tilleggsstonader.sak.utbetaling

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = PollStatusFraUtbetalingTask.TYPE,
    maxAntallFeil = 50,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 31L,
    beskrivelse = "Sjekker status på utbetaling av behandling.",
)
class PollStatusFraUtbetalingTask(
    private val stegService: StegService,
    private val ventePåStatusFraUtbetalingSteg: VentePåStatusFraUtbetalingSteg,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        stegService.håndterSteg(behandlingId, ventePåStatusFraUtbetalingSteg)
    }

    companion object {

        fun opprettTask(behandlingId: UUID): Task =
            Task(
                type = TYPE,
                payload = behandlingId.toString(),
                properties = Properties().apply {
                    setProperty("behandlingId", behandlingId.toString())
                },
            ).copy(triggerTid = LocalDateTime.now().plusSeconds(31))

        const val TYPE = "pollerStatusFraUtbetaling"
    }
}
