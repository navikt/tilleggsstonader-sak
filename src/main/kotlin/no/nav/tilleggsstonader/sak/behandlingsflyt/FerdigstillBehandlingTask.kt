package no.nav.tilleggsstonader.sak.behandlingsflyt

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = FerdigstillBehandlingTask.TYPE,
    maxAntallFeil = 50,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 15 * 60L,
    beskrivelse = "Ferdigstill behandling.",
)
class FerdigstillBehandlingTask(
    private val stegService: StegService,
    private val ferdigstillBehandlingSteg: FerdigstillBehandlingSteg,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        stegService.håndterSteg(behandlingId, ferdigstillBehandlingSteg)
    }

    companion object {

        fun opprettTask(saksbehandling: Saksbehandling): Task =
            Task(
                type = TYPE,
                payload = saksbehandling.id.toString(),
                properties = Properties().apply {
                    setProperty("behandlingId", saksbehandling.id.toString())
                },
            )

        const val TYPE = "ferdigstillBehandling"
    }
}
