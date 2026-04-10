package no.nav.tilleggsstonader.sak.brev.frittstående

import no.nav.tilleggsstonader.kontrakter.dokdist.DistribuerJournalpostRequest
import no.nav.tilleggsstonader.kontrakter.dokdist.Distribusjonstype
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.libs.http.client.ProblemDetailException
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerFrittståendeBrevRepository
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerFrittståendeBrev
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import no.nav.tilleggsstonader.sak.journalføring.JournalpostClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpClientErrorException

@Service
class DistribuerFrittståendeBrevService(
    private val journalpostClient: JournalpostClient,
    private val brevmottakerFrittståendeBrevRepository: BrevmottakerFrittståendeBrevRepository,
    private val transactionHandler: TransactionHandler,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    interface ResultatDistribusjon {
        data object BrevDistribuert : ResultatDistribusjon

        data class FeiletFordiMottakerErDødOgManglerAdresse(
            val feilmelding: String?,
        ) : ResultatDistribusjon
    }

    @Transactional
    fun distribuerBrev(mottaker: BrevmottakerFrittståendeBrev): ResultatDistribusjon {
        try {
            return distribuerBrevOgOppdaterMottakerHvisSuksess(mottaker)
        } catch (ex: ProblemDetailException) {
            logger.warn("Distribusjon av frittstående brev for journalpost ${mottaker.journalpostId} feilet: ${ex.message}")
            if (ex.responseException is HttpClientErrorException.Gone) {
                logger.warn("Kan ikke sende frittstående brev da personen er død og mangler adresse.")
                return ResultatDistribusjon.FeiletFordiMottakerErDødOgManglerAdresse("${ex.message}")
            } else {
                throw ex
            }
        }
    }

    private fun distribuerBrevOgOppdaterMottakerHvisSuksess(mottaker: BrevmottakerFrittståendeBrev): ResultatDistribusjon {
        val bestillingId =
            journalpostClient.distribuerJournalpost(
                DistribuerJournalpostRequest(
                    journalpostId = mottaker.journalpostId ?: error("journalpostId er påkrevd"),
                    bestillendeFagsystem = Fagsystem.TILLEGGSSTONADER,
                    dokumentProdApp = "TILLEGGSSTONADER-SAK",
                    distribusjonstype = Distribusjonstype.VIKTIG,
                ),
            )
        mottaker.lagreDistribusjonGjennomført(bestillingId)
        logger.info(
            "Distribuert frittstående brev (journalpost=${mottaker.journalpostId}) med bestillingId=$bestillingId",
        )
        return ResultatDistribusjon.BrevDistribuert
    }

    private fun BrevmottakerFrittståendeBrev.lagreDistribusjonGjennomført(bestillingId: String) {
        transactionHandler.runInNewTransaction {
            brevmottakerFrittståendeBrevRepository.update(this.copy(bestillingId = bestillingId))
        }
    }
}
