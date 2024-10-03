package no.nav.tilleggsstonader.sak.brev.frittstående

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.kontrakter.dokdist.DistribuerJournalpostRequest
import no.nav.tilleggsstonader.kontrakter.dokdist.Distribusjonstype
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakereFrittståendeBrevService
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.journalføring.JournalpostClient
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
    private val journalpostClient: JournalpostClient,
    private val brevmottakereFrittståendeBrevService: BrevmottakereFrittståendeBrevService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val payload = objectMapper.readValue(task.payload, DistribuerFrittståendeBrevPayload::class.java)

        val bestillingId = journalpostClient.distribuerJournalpost(
            DistribuerJournalpostRequest(
                journalpostId = payload.journalpostId,
                bestillendeFagsystem = Fagsystem.TILLEGGSSTONADER,
                dokumentProdApp = "TILLEGGSSTONADER-SAK",
                distribusjonstype = Distribusjonstype.VIKTIG,
            ),
        )
        brevmottakereFrittståendeBrevService.hentBrevmottakere(payload.mottakerId)
            .copy(bestillingId = bestillingId)
            .let { brevmottakereFrittståendeBrevService.oppdaterBrevmottaker(it) }
    }

    companion object {

        fun opprettTask(fagsakId: FagsakId, journalpostId: String, mottakerId: UUID): Task =
            Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(
                    DistribuerFrittståendeBrevPayload(
                        fagsakId = fagsakId,
                        journalpostId = journalpostId,
                        mottakerId = mottakerId,
                    ),
                ),
                properties = Properties().apply {
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
