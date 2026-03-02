package no.nav.tilleggsstonader.sak.privatbil

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = BehandleMottattKjørelisteTask.TYPE,
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 60L,
    beskrivelse = "Behandler mottatt kjøreliste",
)
class BehandleMottattKjørelisteTask(
    private val kjørelisteService: KjørelisteService,
    private val opprettKjørelisteBehandlingService: BehandleMottattKjørelisteService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val kjøreliste = kjørelisteService.hentKjøreliste(UUID.fromString(task.payload))

        opprettKjørelisteBehandlingService.behandleMottattKjøreliste(kjøreliste)
    }

    companion object {
        const val TYPE = "behandleMottattKjørelisteTask"

        fun opprettTask(kjørelisteId: UUID): Task =
            Task(
                type = TYPE,
                payload = kjørelisteId.toString(),
                properties =
                    Properties().apply {
                        setProperty("kjørelisteId", kjørelisteId.toString())
                    },
            )
    }
}
