package no.nav.tilleggsstonader.sak.brev

import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentRequest
import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentResponse
import no.nav.tilleggsstonader.kontrakter.dokarkiv.Dokument
import no.nav.tilleggsstonader.kontrakter.dokarkiv.Filtype
import no.nav.tilleggsstonader.kontrakter.dokarkiv.dokumenttyper
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerVedtaksbrevRepository
import no.nav.tilleggsstonader.sak.brev.brevmottaker.MottakerUtil.tilAvsenderMottaker
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerVedtaksbrev
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import no.nav.tilleggsstonader.sak.journalføring.ArkiverDokumentConflictException
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class JournalførVedtaksbrevService(
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val journalpostService: JournalpostService,
    private val brevmottakerVedtaksbrevRepository: BrevmottakerVedtaksbrevRepository,
    private val transactionHandler: TransactionHandler,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun journalførForAlleMottakere(
        pdfBytes: ByteArray,
        saksbehandling: Saksbehandling,
        brevmottakere: List<BrevmottakerVedtaksbrev>,
        brevtittel: String,
    ) {
        brevmottakere
            .filter { it.journalpostId == null }
            .forEach { brevmottaker ->
                transactionHandler.runInNewTransaction {
                    journalførForEnMottaker(pdfBytes, saksbehandling, brevmottaker, brevtittel)
                }
            }
    }

    private fun journalførForEnMottaker(
        pdfBytes: ByteArray,
        saksbehandling: Saksbehandling,
        brevmottaker: BrevmottakerVedtaksbrev,
        brevtittel: String,
    ) {
        val dokument =
            Dokument(
                dokument = pdfBytes,
                filtype = Filtype.PDFA,
                dokumenttype = saksbehandling.stønadstype.dokumenttyper.vedtaksbrev,
                tittel = brevtittel,
            )

        val eksternReferanseId = "${saksbehandling.eksternId}-vedtaksbrev-${brevmottaker.id}"

        val arkiverDokumentRequest =
            ArkiverDokumentRequest(
                fnr = saksbehandling.ident,
                forsøkFerdigstill = true,
                hoveddokumentvarianter = listOf(dokument),
                fagsakId = saksbehandling.eksternFagsakId.toString(),
                journalførendeEnhet =
                    arbeidsfordelingService.hentNavEnhet(saksbehandling.ident, saksbehandling.stønadstype)?.enhetNr
                        ?: error("Fant ikke arbeidsfordelingsenhet"),
                eksternReferanseId = eksternReferanseId,
                avsenderMottaker = brevmottaker.mottaker.tilAvsenderMottaker(),
            )

        val response = opprettJournalpost(arkiverDokumentRequest, saksbehandling, eksternReferanseId)
        brevmottakerVedtaksbrevRepository.update(brevmottaker.copy(journalpostId = response.journalpostId))
    }

    private fun opprettJournalpost(
        arkiverDokumentRequest: ArkiverDokumentRequest,
        saksbehandling: Saksbehandling,
        eksternReferanseId: String,
    ): ArkiverDokumentResponse {
        val response =
            try {
                journalpostService.opprettJournalpost(arkiverDokumentRequest)
            } catch (e: ArkiverDokumentConflictException) {
                logger.warn(
                    "Konflikt ved arkivering av dokument. Brevet har sannsynligvis allerede blitt arkivert" +
                        " for behandlingId=${saksbehandling.id} med eksternReferanseId=$eksternReferanseId",
                )
                e.response
            }
        feilHvisIkke(response.ferdigstilt) {
            "Journalposten ble ikke ferdigstilt og kan derfor ikke distribueres"
        }
        return response
    }
}
