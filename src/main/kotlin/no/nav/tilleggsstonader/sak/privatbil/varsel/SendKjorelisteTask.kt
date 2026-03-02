package no.nav.tilleggsstonader.sak.privatbil.varsel

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.PropertiesWrapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.finnMandagNesteUke
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = SendKjorelisteTask.TYPE,
    beskrivelse = "Send varsel om tilgjengelig kjoreliste",
    maxAntallFeil = 1,
)
class SendKjorelisteTask(
    private val notifikasjonsService: VarselDittNavKafkaProducer,
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val taskService: TaskService,
    private val mittNavVarselService: MittNavVarselService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val behandling =
            behandlingService.hentSaksbehandling(BehandlingId.fromString(task.metadata.getProperty("behandlingId")))
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        val fnr = fagsak.hentAktivIdent()

        val melding = "Du har én eller flere kjørelister tilgjengelige for utfylling."
        val eventId = task.payload

        if (mittNavVarselService.skalOppretteKjørelisteVarselTask(behandling)) {
            notifikasjonsService.sendToKafka(fnr, melding, eventId)
            taskService.save(
                opprettTask(
                    BehandlingId.fromString(task.metadata.getProperty("behandlingId")),
                ),
            )
        }
    }

    companion object {
        const val TYPE = "SEND_NOTIFIKASJON"

        fun opprettTask(behandlingId: BehandlingId): Task {
            val properties =
                Properties().apply {
                    setProperty("behandlingId", behandlingId.toString())
                }
            // Må gjøre dette for å ikke få duplicate på payload
            return Task(TYPE, UUID.randomUUID().toString())
                .copy(metadataWrapper = PropertiesWrapper(properties))
                .medTriggerTid(LocalDate.now().finnMandagNesteUke().atTime(10, 0))
        }
    }
}
