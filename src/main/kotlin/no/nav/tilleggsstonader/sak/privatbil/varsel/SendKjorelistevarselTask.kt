package no.nav.tilleggsstonader.sak.privatbil.varsel

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = SendKjorelistevarselTask.TYPE,
    beskrivelse = "Send varsel om tilgjengelig kjoreliste",
    maxAntallFeil = 1,
)
class SendKjorelistevarselTask : AsyncTaskStep {
    override fun doTask(task: Task) {
        // Slett etter alle tasker har kjørt?
    }

    companion object {
        const val TYPE = "sendKjorelistevarselTask"
    }
}
