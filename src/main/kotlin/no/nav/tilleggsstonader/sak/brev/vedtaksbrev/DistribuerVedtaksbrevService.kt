package no.nav.tilleggsstonader.sak.brev.vedtaksbrev

import no.nav.tilleggsstonader.kontrakter.dokdist.DistribuerJournalpostRequest
import no.nav.tilleggsstonader.kontrakter.dokdist.Distribusjonstype
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.libs.http.client.ProblemDetailException
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerVedtaksbrevRepository
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerVedtaksbrev
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import no.nav.tilleggsstonader.sak.journalføring.JournalpostClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpClientErrorException

@Service
class DistribuerVedtaksbrevService(
    private val journalpostClient: JournalpostClient,
    private val brevmottakerVedtaksbrevRepository: BrevmottakerVedtaksbrevRepository,
    private val transactionHandler: TransactionHandler,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    interface ResultatBrevutsendelse {
        data object BrevDistribuert : ResultatBrevutsendelse

        data class FeiletFordiMottakerErDødOgManglerAdresse(
            val feilmelding: String?,
        ) : ResultatBrevutsendelse
    }

    @Transactional
    fun distribuerVedtaksbrev(mottaker: BrevmottakerVedtaksbrev): ResultatBrevutsendelse {
        try {
            return distribuerVedtaksbrevOgOppdaterMottakerHvisSuksess(mottaker)
        } catch (ex: ProblemDetailException) {
            logger.warn("Distribusjon av vedtaksbrev for journalpost ${mottaker.journalpostId} feilet: ${ex.message}")
            if (ex.responseException is HttpClientErrorException.Gone) {
                logger.warn("Kan ikke sende vedtaksbrev da personen er død og mangler adresse.")
                return ResultatBrevutsendelse.FeiletFordiMottakerErDødOgManglerAdresse("${ex.message}")
            } else {
                throw ex
            }
        }
    }

    private fun distribuerVedtaksbrevOgOppdaterMottakerHvisSuksess(mottaker: BrevmottakerVedtaksbrev): ResultatBrevutsendelse {
        val bestillingsId =
            journalpostClient.distribuerJournalpost(
                DistribuerJournalpostRequest(
                    journalpostId = mottaker.journalpostId ?: error("journalpostId er påkrevd"),
                    bestillendeFagsystem = Fagsystem.TILLEGGSSTONADER,
                    dokumentProdApp = "TILLEGGSSTONADER-SAK",
                    distribusjonstype = Distribusjonstype.VEDTAK,
                ),
            )
        mottaker.lagreDistribusjonGjennomført(bestillingsId)
        logger.info(
            "Distribuert vedtaksbrev (journalpost=${mottaker.journalpostId}) med bestillingId=$bestillingsId",
        )
        return ResultatBrevutsendelse.BrevDistribuert
    }

    private fun BrevmottakerVedtaksbrev.lagreDistribusjonGjennomført(bestillingId: String) {
        transactionHandler.runInNewTransaction {
            brevmottakerVedtaksbrevRepository.update(this.copy(bestillingId = bestillingId))
        }
    }
}
