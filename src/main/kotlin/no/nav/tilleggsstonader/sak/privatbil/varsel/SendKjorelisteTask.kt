package no.nav.tilleggsstonader.sak.privatbil.varsel

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = SendKjorelisteTask.TYPE,
    beskrivelse = "Send varsel om tilgjengelig kjoreliste",
    maxAntallFeil = 3,
)
class SendKjorelisteTask(
    private val notifikasjonsService: VarselDittNavKafkaProducer,
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val behandling = behandlingService.hentBehandling(BehandlingId.fromString(task.payload))
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        val fnr = fagsak.hentAktivIdent()

        val melding = "Du har én eller flere kjørelister tilgjengelige for utfylling."
        // TODO: finne en eventId som er en gyldig UUID og unik
        val eventId = task.payload

        notifikasjonsService.sendToKafka(fnr, melding, eventId)
    }

    companion object {
        const val TYPE = "SEND_NOTIFIKASJON"

        fun opprettTask(behandlingId: BehandlingId): Task {
            Properties().apply {
                setProperty("behandlingId", behandlingId.toString())
            }
            return Task(TYPE, behandlingId.toString())
            // TODO: hvordan sjekker jeg for dette i integrasjonstesten?
            // .medTriggerTid(LocalDate.now().finnMandagNesteUke().atTime(10, 0))
        }
    }
}
