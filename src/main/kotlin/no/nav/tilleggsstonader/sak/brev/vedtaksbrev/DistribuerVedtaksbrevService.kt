package no.nav.tilleggsstonader.sak.brev.vedtaksbrev

import no.nav.tilleggsstonader.kontrakter.dokdist.Distribusjonstype
import no.nav.tilleggsstonader.sak.brev.DistribuerBrevService
import no.nav.tilleggsstonader.sak.brev.ResultatDistribusjon
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerVedtaksbrevRepository
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerVedtaksbrev
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feil
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import org.springframework.stereotype.Service

@Service
class DistribuerVedtaksbrevService(
    private val brevmottakerVedtaksbrevRepository: BrevmottakerVedtaksbrevRepository,
    private val transactionHandler: TransactionHandler,
    private val distribuerBrevService: DistribuerBrevService,
) {
    fun distribuerVedtaksbrev(mottaker: BrevmottakerVedtaksbrev): ResultatDistribusjon =
        distribuerBrevService.distribuerOgHåndterDødsbo(
            journalpostId = mottaker.journalpostId ?: feil("journalpostId er påkrevd"),
            distribusjonstype = Distribusjonstype.VEDTAK,
            brevtype = "vedtaksbrev",
        ) { bestillingId -> mottaker.lagreDistribusjonGjennomført(bestillingId) }

    private fun BrevmottakerVedtaksbrev.lagreDistribusjonGjennomført(bestillingId: String) {
        transactionHandler.runInNewTransaction {
            brevmottakerVedtaksbrevRepository.update(this.copy(bestillingId = bestillingId))
        }
    }
}
