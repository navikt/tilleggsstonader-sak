package no.nav.tilleggsstonader.sak.brev

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentRequest
import no.nav.tilleggsstonader.kontrakter.dokarkiv.Dokument
import no.nav.tilleggsstonader.kontrakter.dokarkiv.Dokumenttype
import no.nav.tilleggsstonader.kontrakter.dokarkiv.Filtype
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FrittståendeBrevService(
    private val familieDokumentClient: FamilieDokumentClient,
    private val fagsakService: FagsakService,
    private val journalpostService: JournalpostService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val taskService: TaskService,
) {

    fun lagFrittståendeSanitybrev(
        request: GenererPdfRequest,
    ): ByteArray {
        val signatur = SikkerhetContext.hentSaksbehandlerNavn(strict = true)

        val htmlMedSignatur = BrevUtil.settInnSaksbehandlerSignaturOgDato(request.html, signatur)

        return familieDokumentClient.genererPdf(htmlMedSignatur)
    }

    fun sendFrittståendeBrev(
        fagsakId: UUID,
        request: FrittståendeBrevDto,
    ) {
        val fagsak = fagsakService.hentFagsak(fagsakId)

        val journalpostId = journalførFrittståendeBrev(request, fagsak)

        taskService.save(DistribuerFrittståendeBrevTask.opprettTask(fagsakId, journalpostId))
    }

    private fun journalførFrittståendeBrev(
        request: FrittståendeBrevDto,
        fagsak: Fagsak,
    ): String {
        val dokument = Dokument(
            dokument = request.pdf,
            filtype = Filtype.PDFA,
            dokumenttype = utledDokumenttype(fagsak.stønadstype),
            tittel = request.tittel,
        )

        val saksbehandler = SikkerhetContext.hentSaksbehandler()

        val arkiverDokumentRequest = ArkiverDokumentRequest(
            fnr = fagsak.hentAktivIdent(),
            eksternReferanseId = UUID.randomUUID().toString(), // TODO Finn ut hva som bør brukes her
            forsøkFerdigstill = true,
            hoveddokumentvarianter = listOf(dokument),
            fagsakId = fagsak.eksternId.id.toString(),
            journalførendeEnhet = arbeidsfordelingService.hentNavEnhet(saksbehandler)?.enhetId
                ?: error("Fant ikke arbeidsfordelingsenhet"),
        )

        return journalpostService.opprettJournalpost(arkiverDokumentRequest, saksbehandler).journalpostId
    }

    private fun utledDokumenttype(stønadstype: Stønadstype) =
        when (stønadstype) {
            Stønadstype.BARNETILSYN -> Dokumenttype.BARNETILSYN_FRITTSTÅENDE_BREV
        }
}
