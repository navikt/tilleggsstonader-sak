package no.nav.tilleggsstonader.sak.brev.kjørelistebrev

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.kontrakter.dokdist.DistribuerJournalpostRequest
import no.nav.tilleggsstonader.kontrakter.dokdist.Distribusjonstype
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.journalføring.JournalpostClient
import org.springframework.stereotype.Service
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = DistribuerKjørelisteBehandlingBrevTask.TYPE,
    maxAntallFeil = 50,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 3 * 60L,
    beskrivelse = "Distribuerer kjørelistebrev etter journalføring",
)
class DistribuerKjørelisteBehandlingBrevTask(
    private val journalpostClient: JournalpostClient,
    private val kjørelisteBehandlingBrevService: KjørelisteBehandlingBrevService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val behandlingId = BehandlingId.fromString(task.payload)
        val brev = kjørelisteBehandlingBrevService.hentLagretBrev(behandlingId)

//        val journalpostId = brev.journalpostId ?: error("Mangler journalpostId på kjørelistebrev for behandling=$behandlingId")

//        val bestillingId =
//            journalpostClient.distribuerJournalpost(
//                DistribuerJournalpostRequest(
//                    journalpostId = journalpostId,
//                    bestillendeFagsystem = Fagsystem.TILLEGGSSTONADER,
//                    dokumentProdApp = "TILLEGGSSTONADER-SAK",
//                    distribusjonstype = Distribusjonstype.VIKTIG,
//                ),
//            )

//        kjørelisteBehandlingBrevService.lagreBestillingId(behandlingId, bestillingId)
    }

    companion object {
        fun opprettTask(behandlingId: BehandlingId): Task =
            Task(
                type = TYPE,
                payload = behandlingId.toString(),
                properties =
                    Properties().apply {
                        setProperty("behandlingId", behandlingId.toString())
                    },
            )

        const val TYPE = "distribuerKjørelistebrev"
    }
}
