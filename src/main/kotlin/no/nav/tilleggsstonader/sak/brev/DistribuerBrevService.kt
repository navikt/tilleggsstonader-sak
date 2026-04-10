package no.nav.tilleggsstonader.sak.brev

import no.nav.tilleggsstonader.kontrakter.dokdist.DistribuerJournalpostRequest
import no.nav.tilleggsstonader.kontrakter.dokdist.Distribusjonstype
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.libs.http.client.ProblemDetailException
import no.nav.tilleggsstonader.sak.journalføring.JournalpostClient
import no.nav.tilleggsstonader.sak.opplysninger.pdl.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.HttpClientErrorException

@Service
class DistribuerBrevService(
    val journalpostClient: JournalpostClient,
) {
    @Transactional
    fun distribuerOgHåndterDødsbo(
        journalpostId: String,
        distribusjonstype: Distribusjonstype,
        brevtype: String,
        lagreDistribusjon: (bestillingId: String) -> Unit,
    ): ResultatDistribusjon =
        try {
            val bestillingId =
                journalpostClient.distribuerJournalpost(
                    DistribuerJournalpostRequest(
                        journalpostId = journalpostId,
                        bestillendeFagsystem = Fagsystem.TILLEGGSSTONADER,
                        dokumentProdApp = "TILLEGGSSTONADER-SAK",
                        distribusjonstype = distribusjonstype,
                    ),
                )
            lagreDistribusjon(bestillingId)
            logger.info("Distribuert $brevtype (journalpost=$journalpostId) med bestillingId=$bestillingId")
            ResultatDistribusjon.Distribuert
        } catch (ex: ProblemDetailException) {
            logger.warn("Distribusjon av $brevtype for journalpost $journalpostId feilet: ${ex.message}")
            if (ex.responseException is HttpClientErrorException.Gone) {
                logger.warn("Kan ikke sende $brevtype da personen er død og mangler adresse.")
                ResultatDistribusjon.FeiletFordiMottakerErDødOgManglerAdresse(ex.message)
            } else {
                throw ex
            }
        }
}
