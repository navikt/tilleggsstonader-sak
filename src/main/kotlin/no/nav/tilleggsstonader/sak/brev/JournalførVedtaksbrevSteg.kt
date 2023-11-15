package no.nav.tilleggsstonader.sak.brev

import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentRequest
import no.nav.tilleggsstonader.kontrakter.dokarkiv.Dokument
import no.nav.tilleggsstonader.kontrakter.dokarkiv.Dokumenttype
import no.nav.tilleggsstonader.kontrakter.dokarkiv.Filtype
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException

@Service
class JournalførVedtaksbrevSteg(
    private val brevService: BrevService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val journalpostService: JournalpostService,
) : BehandlingSteg<Void?> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun utførSteg(saksbehandling: Saksbehandling, data: Void?) {
        val vedtaksbrev = brevService.hentBesluttetBrev(saksbehandling.id)

        val dokument = Dokument(
            dokument = vedtaksbrev.beslutterPdf?.bytes ?: error("Mangler beslutterpdf"),
            filtype = Filtype.PDFA,
            dokumenttype = utledDokumenttype(saksbehandling),
            tittel = utledBrevtittel(saksbehandling),
        )

        val eksternReferanseId = "${saksbehandling.eksternId}-vedtaksbrev"

        val arkviverDokumentRequest = ArkiverDokumentRequest(
            fnr = saksbehandling.ident,
            forsøkFerdigstill = true,
            hoveddokumentvarianter = listOf(dokument),
            fagsakId = saksbehandling.eksternFagsakId.toString(),
            journalførendeEnhet = arbeidsfordelingService.hentNavEnhet(saksbehandling.ident)?.enhetId
                ?: error("Fant ikke arbeidsfordelingsenhet"),
            eksternReferanseId = eksternReferanseId,
        )

        try {
            journalpostService.opprettJournalpost(arkviverDokumentRequest)
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.CONFLICT) {
                logger.warn("Konflikt ved arkivering av dokument. Vedtaksbrevet har sannsynligvis allerede blitt arkivert for behandlingId=${saksbehandling.id} med eksternReferanseId=$eksternReferanseId")
            } else {
                throw e
            }
        }
    }

    private fun utledBrevtittel(saksbehandling: Saksbehandling) = when (saksbehandling.stønadstype) {
        Stønadstype.BARNETILSYN -> "Vedtak om stønad til tilsyn barn" // TODO
    }

    private fun utledDokumenttype(saksbehandling: Saksbehandling) =
        when (saksbehandling.stønadstype) {
            Stønadstype.BARNETILSYN -> Dokumenttype.BARNETILSYN_VEDTAKSBREV
        }

    override fun stegType(): StegType = StegType.JOURNALFØRE_VEDTAKSBREV
}
