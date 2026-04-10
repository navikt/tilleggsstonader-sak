package no.nav.tilleggsstonader.sak.brev.frittstående

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerFrittståendeBrevRepository
import no.nav.tilleggsstonader.sak.brev.frittstående.DistribuerFrittståendeBrevService.ResultatDistribusjon
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.stoppTaskOgRekjørSenere
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = DistribuerFrittståendeBrevTask.TYPE,
    maxAntallFeil = 50,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 31L,
    beskrivelse = "Distribuerer frittstående brev etter journalføring",
)
class DistribuerFrittståendeBrevTask(
    private val brevmottakerFrittståendeBrevRepository: BrevmottakerFrittståendeBrevRepository,
    private val distribuerFrittståendeBrevService: DistribuerFrittståendeBrevService,
    private val taskService: TaskService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val payload = jsonMapper.readValue(task.payload, DistribuerFrittståendeBrevPayload::class.java)

        val mottaker = brevmottakerFrittståendeBrevRepository.findByIdOrThrow(payload.mottakerId)

        distribuerFrittståendeBrevService
            .distribuerBrev(mottaker)
            .håndterRekjøringSenereHvisMottakerErDød(task)
    }

    private fun ResultatDistribusjon.håndterRekjøringSenereHvisMottakerErDød(task: Task) {
        if (this is ResultatDistribusjon.FeiletFordiMottakerErDødOgManglerAdresse) {
            taskService.stoppTaskOgRekjørSenere(task, årsak = "Mottaker er død", melding = feilmelding)
        }
    }

    companion object {
        fun opprettTask(
            fagsakId: FagsakId,
            journalpostId: String,
            mottakerId: UUID,
        ): Task =
            Task(
                type = TYPE,
                payload =
                    jsonMapper.writeValueAsString(
                        DistribuerFrittståendeBrevPayload(
                            fagsakId = fagsakId,
                            journalpostId = journalpostId,
                            mottakerId = mottakerId,
                        ),
                    ),
                properties =
                    Properties().apply {
                        setProperty("fagsakId", fagsakId.toString())
                        setProperty("journalpostId", journalpostId)
                        setProperty("mottakerId", mottakerId.toString())
                    },
            )

        const val TYPE = "distribuerFrittståendeBrev"
    }
}

data class DistribuerFrittståendeBrevPayload(
    val fagsakId: FagsakId,
    val journalpostId: String,
    val mottakerId: UUID,
)
