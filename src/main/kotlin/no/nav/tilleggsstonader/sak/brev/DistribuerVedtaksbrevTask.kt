package no.nav.tilleggsstonader.sak.brev

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.tilleggsstonader.kontrakter.dokdist.DistribuerJournalpostRequest
import no.nav.tilleggsstonader.kontrakter.dokdist.Distribusjonstype
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.brev.brevmottaker.Brevmottaker
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerRepository
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import no.nav.tilleggsstonader.sak.journalføring.JournalpostClient
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = DistribuerVedtaksbrevTask.TYPE,
    maxAntallFeil = 50,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 31L,
    beskrivelse = "Distribuerer vedtaksbrev etter journalføring",
)
class DistribuerVedtaksbrevTask(
    private val brevmottakerRepository: BrevmottakerRepository,
    private val journalpostClient: JournalpostClient,
    private val stegService: StegService,
    private val brevSteg: BrevSteg,
    private val transactionHandler: TransactionHandler,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)

        val brevmottakere = brevmottakerRepository.findByBehandlingId(behandlingId)

        validerHarBrevmottakere(brevmottakere)

        brevmottakere
            .filter { it.bestillingId == null }
            .forEach { brevmottaker ->
                val bestillingId = distribuerTilBrevmottaker(brevmottaker)

                transactionHandler.runInNewTransaction {
                    brevmottakerRepository.update(brevmottaker.copy(bestillingId = bestillingId))
                }
            }

        stegService.håndterSteg(behandlingId, brevSteg)
    }

    private fun distribuerTilBrevmottaker(it: Brevmottaker) = journalpostClient.distribuerJournalpost(
        DistribuerJournalpostRequest(
            journalpostId = it.journalpostId
                ?: error("Ugyldig tilstand. Mangler journalpostId for brev som skal distribueres"),
            bestillendeFagsystem = Fagsystem.TILLEGGSSTONADER,
            dokumentProdApp = "TILLEGGSSTONADER-SAK",
            distribusjonstype = Distribusjonstype.VEDTAK,
        ),
    )

    private fun validerHarBrevmottakere(brevmottakere: List<Brevmottaker>) {
        feilHvis(brevmottakere.isEmpty()) {
            "Ingen brevmottakere"
        }
    }

    companion object {

        fun opprettTask(behandlingId: UUID): Task =
            Task(
                type = TYPE,
                payload = behandlingId.toString(),
                properties = Properties().apply {
                    setProperty("behandlingId", behandlingId.toString())
                },
            )

        const val TYPE = "distribuerVedtaksbrev"
    }
}
