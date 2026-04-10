package no.nav.tilleggsstonader.sak.brev.frittstående

import no.nav.tilleggsstonader.kontrakter.dokdist.Distribusjonstype
import no.nav.tilleggsstonader.sak.brev.DistribuerBrevService
import no.nav.tilleggsstonader.sak.brev.ResultatDistribusjon
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerFrittståendeBrevRepository
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerFrittståendeBrev
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import org.springframework.stereotype.Service

@Service
class DistribuerFrittståendeBrevService(
    private val brevmottakerFrittståendeBrevRepository: BrevmottakerFrittståendeBrevRepository,
    private val transactionHandler: TransactionHandler,
    private val distribuerBrevService: DistribuerBrevService,
) {
    fun distribuerBrev(mottaker: BrevmottakerFrittståendeBrev): ResultatDistribusjon =
        distribuerBrevService.distribuerOgHåndterDødsbo(
            journalpostId = mottaker.journalpostId ?: error("journalpostId er påkrevd"),
            distribusjonstype = Distribusjonstype.VIKTIG,
            brevtype = "frittstående brev",
        ) { bestillingId -> mottaker.lagreDistribusjonGjennomført(bestillingId) }

    private fun BrevmottakerFrittståendeBrev.lagreDistribusjonGjennomført(bestillingId: String) {
        transactionHandler.runInNewTransaction {
            brevmottakerFrittståendeBrevRepository.update(this.copy(bestillingId = bestillingId))
        }
    }
}
