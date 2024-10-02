package no.nav.tilleggsstonader.sak.brev

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentRequest
import no.nav.tilleggsstonader.kontrakter.dokarkiv.AvsenderMottaker
import no.nav.tilleggsstonader.kontrakter.dokarkiv.Dokument
import no.nav.tilleggsstonader.kontrakter.dokarkiv.Dokumenttype
import no.nav.tilleggsstonader.kontrakter.dokarkiv.Filtype
import no.nav.tilleggsstonader.kontrakter.felles.BrukerIdType
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.log.mdc.MDCConstants
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.brev.mellomlager.MellomlagringBrevService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.journalføring.FamilieDokumentClient
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import org.slf4j.MDC
import org.springframework.stereotype.Service

@Service
class FrittståendeBrevService(
    private val familieDokumentClient: FamilieDokumentClient,
    private val fagsakService: FagsakService,
    private val journalpostService: JournalpostService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val taskService: TaskService,
    private val mellomlagringBrevService: MellomlagringBrevService,
) {

    fun lagFrittståendeSanitybrev(
        request: GenererPdfRequest,
    ): ByteArray {
        val signatur = SikkerhetContext.hentSaksbehandlerNavn(strict = true)

        val htmlMedSignatur = BrevUtil.settInnSaksbehandlerSignaturOgDato(request.html, signatur)

        return familieDokumentClient.genererPdf(htmlMedSignatur)
    }

    fun sendFrittståendeBrev(
        fagsakId: FagsakId,
        request: FrittståendeBrevDto,
    ) {
        val fagsak = fagsakService.hentFagsak(fagsakId)
        val saksbehandler = SikkerhetContext.hentSaksbehandler()

        val journalpostId = journalførFrittståendeBrev(request, fagsak, saksbehandler)

        taskService.save(DistribuerFrittståendeBrevTask.opprettTask(fagsakId, journalpostId))

        mellomlagringBrevService.slettMellomlagretFrittståendeBrev(fagsakId, saksbehandler)
    }

    private fun journalførFrittståendeBrev(
        request: FrittståendeBrevDto,
        fagsak: Fagsak,
        saksbehandler: String,
    ): String {
        val dokument = Dokument(
            dokument = request.pdf,
            filtype = Filtype.PDFA,
            dokumenttype = utledDokumenttype(fagsak.stønadstype),
            tittel = request.tittel,
        )

        val ident = fagsak.hentAktivIdent()
        val arkiverDokumentRequest = ArkiverDokumentRequest(
            fnr = ident,
            eksternReferanseId = MDC.get(MDCConstants.MDC_CALL_ID) ?: throw IllegalStateException("Mangler callId"),
            forsøkFerdigstill = true,
            hoveddokumentvarianter = listOf(dokument),
            fagsakId = fagsak.eksternId.id.toString(),
            journalførendeEnhet = arbeidsfordelingService.hentNavEnhet(ident)?.enhetNr
                ?: error("Fant ikke arbeidsfordelingsenhet"),
            avsenderMottaker = AvsenderMottaker(id = ident, idType = BrukerIdType.FNR, navn = null),
        )

        val journalpost = journalpostService.opprettJournalpost(arkiverDokumentRequest, saksbehandler)

        feilHvisIkke(journalpost.ferdigstilt) { "Journalpost er ikke ferdigstilt" }

        return journalpost.journalpostId
    }

    private fun utledDokumenttype(stønadstype: Stønadstype) =
        when (stønadstype) {
            Stønadstype.BARNETILSYN -> Dokumenttype.BARNETILSYN_FRITTSTÅENDE_BREV
            Stønadstype.LÆREMIDLER -> Dokumenttype.LÆREMIDLER_FRITTSTÅENDE_BREV
            else -> error("Frittstående brev er ikke støttet for stønadstype $stønadstype")
        }
}
