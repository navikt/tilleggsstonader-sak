package no.nav.tilleggsstonader.sak.brev.vedtaksbrev

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.dokdist.DistribuerJournalpostRequest
import no.nav.tilleggsstonader.kontrakter.dokdist.Distribusjonstype
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.libs.http.client.ProblemDetailException
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerVedtaksbrevRepository
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerVedtaksbrev
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import no.nav.tilleggsstonader.sak.journalføring.JournalpostClient
import no.nav.tilleggsstonader.sak.util.stoppTaskOgRekjørSenere
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import java.util.Properties

/**
 * Distribuering av vedtaksbrev skjer asynkront etter at journalposten er opprettet.
 * Det er ikke en del av behandlingsflyten sånn at en distribuering ikke skal stoppe behandlingen.
 *
 * I tilfelle en person er død, vil distribueringen feile med 410 Gone. Og denne tasken vil da kjøre på nytt.
 */
@Service
@TaskStepBeskrivelse(
    taskStepType = DistribuerVedtaksbrevTask.TYPE,
    maxAntallFeil = 75,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 3 * 60L,
    beskrivelse = "Distribuerer vedtaksbrev etter journalføring",
)
class DistribuerVedtaksbrevTask(
    private val brevmottakerVedtaksbrevRepository: BrevmottakerVedtaksbrevRepository,
    private val journalpostClient: JournalpostClient,
    private val transactionHandler: TransactionHandler,
    private val taskService: TaskService,
) : AsyncTaskStep {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        val behandlingId = BehandlingId.fromString(task.payload)

        brevmottakerVedtaksbrevRepository
            .findByBehandlingId(behandlingId)
            .ifEmpty { throw Feil("Ingen brevmottakere funnet") }
            .filter { it.harIkkeFåttBrevet() }
            .map { distribuerBrevet(mottaker = it) }
            .håndterRekjøringSenereHvisMottakerErDød(task)
    }

    private sealed interface ResultatBrevutsendelse {
        data object BrevDistribuert : ResultatBrevutsendelse

        data class FeiletFordiMottakerErDød(
            val feilmelding: String?,
        ) : ResultatBrevutsendelse
    }

    private fun distribuerBrevet(mottaker: BrevmottakerVedtaksbrev): ResultatBrevutsendelse {
        feilHvis(mottaker.journalpostId == null) { "Ugyldig tilstand. Mangler journalpostId for brev som skal distribueres" }

        try {
            val bestillingId = distribuerVedtaksbrev(mottaker.journalpostId)
            mottaker.lagreDistribusjonGjennomført(bestillingId)
            return ResultatBrevutsendelse.BrevDistribuert
        } catch (ex: ProblemDetailException) {
            logger.warn("Distribusjon av vedtaksbrev for journalpost ${mottaker.journalpostId} feilet: ${ex.message}")
            if (ex.responseException is HttpClientErrorException.Gone) {
                return ResultatBrevutsendelse.FeiletFordiMottakerErDød(ex.message)
            } else {
                throw ex
            }
        }
    }

    private fun BrevmottakerVedtaksbrev.lagreDistribusjonGjennomført(bestillingId: String) {
        transactionHandler.runInNewTransaction {
            brevmottakerVedtaksbrevRepository.update(this.copy(bestillingId = bestillingId))
        }
        logger.info(
            "Distribuert vedtaksbrev (journalpost=$journalpostId) med bestillingId=$bestillingId",
        )
    }

    private fun distribuerVedtaksbrev(journalpostId: String): String =
        journalpostClient.distribuerJournalpost(
            DistribuerJournalpostRequest(
                journalpostId = journalpostId,
                bestillendeFagsystem = Fagsystem.TILLEGGSSTONADER,
                dokumentProdApp = "TILLEGGSSTONADER-SAK",
                distribusjonstype = Distribusjonstype.VEDTAK,
            ),
        )

    private fun List<ResultatBrevutsendelse>.håndterRekjøringSenereHvisMottakerErDød(task: Task) {
        filterIsInstance<ResultatBrevutsendelse.FeiletFordiMottakerErDød>()
            .firstOrNull()
            ?.let { taskService.stoppTaskOgRekjørSenere(task, årsak = "Mottaker er død", melding = it.feilmelding) }
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

        const val TYPE = "distribuerVedtaksbrev"
    }
}
