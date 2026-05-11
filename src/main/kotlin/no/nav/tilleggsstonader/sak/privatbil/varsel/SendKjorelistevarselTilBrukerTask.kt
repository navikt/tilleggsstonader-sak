package no.nav.tilleggsstonader.sak.privatbil.varsel

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import org.springframework.stereotype.Service
import tools.jackson.module.kotlin.readValue
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = SendKjorelistevarselTilBrukerTask.TYPE,
    beskrivelse = "Send varsel om tilgjengelig kjoreliste",
    maxAntallFeil = 1,
)
class SendKjorelistevarselTilBrukerTask(
    private val varselDittNavKafkaProducer: VarselDittNavKafkaProducer,
    private val fagsakPersonService: FagsakPersonService,
) : AsyncTaskStep {
    data class SendKjørelistevarselTilBrukerTaskData(
        val varselId: UUID,
        val fagsakPersonId: FagsakPersonId,
    )

    override fun doTask(task: Task) {
        val taskData = jsonMapper.readValue<SendKjørelistevarselTilBrukerTaskData>(task.payload)
        // TODO -logg
        varselDittNavKafkaProducer.sendVarselOmKjørelisterTilgjengelig(
            fnr = fagsakPersonService.hentAktivIdent(taskData.fagsakPersonId),
            varselId = taskData.varselId.toString(),
        )
    }

    companion object {
        const val TYPE = "sendKjorelistevarselTilBrukerTask"

        fun opprett(fagsakPersonId: FagsakPersonId) =
            Task(
                type = TYPE,
                payload =
                    jsonMapper.writeValueAsString(
                        SendKjørelistevarselTilBrukerTaskData(
                            varselId = UUID.randomUUID(),
                            fagsakPersonId = fagsakPersonId,
                        ),
                    ),
            )
    }
}
