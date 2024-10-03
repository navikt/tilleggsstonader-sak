package no.nav.tilleggsstonader.sak.brev.frittstående

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.kontrakter.dokdist.DistribuerJournalpostRequest
import no.nav.tilleggsstonader.kontrakter.dokdist.Distribusjonstype
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.journalføring.JournalpostClient
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = DistribuerFrittståendeBrevTask.TYPE,
    maxAntallFeil = 50,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 31L,
    beskrivelse = "Distribuerer frittstående brev etter journalføring",
)
class DistribuerFrittståendeBrevTask(
    private val journalpostClient: JournalpostClient,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val payload = objectMapper.readValue(task.payload, DistribuerFrittståendeBrevPayload::class.java)

        journalpostClient.distribuerJournalpost(
            DistribuerJournalpostRequest(
                journalpostId = payload.journalpostId,
                bestillendeFagsystem = Fagsystem.TILLEGGSSTONADER,
                dokumentProdApp = "TILLEGGSSTONADER-SAK",
                distribusjonstype = Distribusjonstype.VIKTIG,
            ),
        )
    }

    companion object {

        fun opprettTask(fagsakId: FagsakId, journalpostId: String): Task =
            Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(
                    DistribuerFrittståendeBrevPayload(
                        fagsakId = fagsakId,
                        journalpostId = journalpostId,
                    ),
                ),
                properties = Properties().apply {
                    setProperty("fagsakId", fagsakId.toString())
                    setProperty("journalpostId", journalpostId)
                },
            )

        const val TYPE = "frittståendeBrev"
    }
}

data class DistribuerFrittståendeBrevPayload(
    val fagsakId: FagsakId,
    val journalpostId: String,
)
