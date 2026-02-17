package no.nav.tilleggsstonader.sak.privatbil

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.kontrakter.felles.Skjematype
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørelisteService
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(taskStepType = SendKjorelisteTask.TYPE, beskrivelse = "Send varsel om tilgjengelig kjoreliste")
class SendKjorelisteTask(
    private val notifikasjonsService: DittNavKafkaProducer,
    private val avklartKjørelisteService: AvklartKjørelisteService
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        // TODO: Hent kjøreliste og send den i meldingen
        val kjøreliste = avklartKjørelisteService.hentAvklarteUkerForBehandling(BehandlingId.fromString(task.payload))
        val melding = "Du har én eller flere kjørelister tilgjengelige for utfylling."
        val eventId = kjøreliste.firstOrNull()?.kjørelisteId.toString()
        notifikasjonsService.sendToKafka(melding, eventId)
    }

    companion object {
        const val TYPE = "SEND_NOTIFIKASJON"

        fun opprettTask(
            kjørelisteId: String
        ): Task {
            val properties =
                Properties().apply {
                    setProperty("kjørelisteId", kjørelisteId )
                    setProperty("type", Skjematype.DAGLIG_REISE_KJØRELISTE.name)
                }
            return Task(TYPE, kjørelisteId, properties)
        }
    }
}
