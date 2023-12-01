package no.nav.tilleggsstonader.sak.journalføring

import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentRequest
import no.nav.tilleggsstonader.kontrakter.dokarkiv.ArkiverDokumentResponse
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.kontrakter.journalpost.Journalstatus
import no.nav.tilleggsstonader.kontrakter.sak.DokumentBrevkode
import no.nav.tilleggsstonader.kontrakter.søknad.Søknadsskjema
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.SøknadsskjemaBarnetilsyn
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import org.springframework.stereotype.Service

@Service
class JournalpostService(private val journalpostClient: JournalpostClient) {

    fun hentJournalpost(journalpostId: String): Journalpost {
        return journalpostClient.hentJournalpost(journalpostId)
    }

    fun opprettJournalpost(arkiverDokumentRequest: ArkiverDokumentRequest, saksbehandler: String? = null): ArkiverDokumentResponse {
        return journalpostClient.opprettJournalpost(arkiverDokumentRequest, saksbehandler)
    }

    fun oppdaterOgFerdigstillJournalpostMaskinelt(
        journalpost: Journalpost,
        journalførendeEnhet: String,
        fagsak: Fagsak,
    ) = oppdaterOgFerdigstillJournalpost(
        journalpost = journalpost,
        dokumenttitler = null,
        journalførendeEnhet = journalførendeEnhet,
        fagsak = fagsak,
        saksbehandler = null,
    )

    fun oppdaterOgFerdigstillJournalpost(
        journalpost: Journalpost,
        dokumenttitler: Map<String, String>?,
        journalførendeEnhet: String,
        fagsak: Fagsak,
        saksbehandler: String?,
    ) {
        if (journalpost.journalstatus != Journalstatus.JOURNALFOERT) {
            oppdaterJournalpostMedFagsakOgDokumenttitler(
                journalpost = journalpost,
                dokumenttitler = dokumenttitler,
                eksternFagsakId = fagsak.eksternId.id,
                saksbehandler = saksbehandler,
            )
            ferdigstillJournalføring(
                journalpostId = journalpost.journalpostId,
                journalførendeEnhet = journalførendeEnhet,
                saksbehandler = saksbehandler,
            )
        }
    }

    private fun ferdigstillJournalføring(journalpostId: String, journalførendeEnhet: String, saksbehandler: String? = null) {
        journalpostClient.ferdigstillJournalpost(journalpostId, journalførendeEnhet, saksbehandler)
    }

    private fun oppdaterJournalpostMedFagsakOgDokumenttitler(
        journalpost: Journalpost,
        dokumenttitler: Map<String, String>? = null,
        eksternFagsakId: Long,
        saksbehandler: String? = null,
    ) {
        val oppdatertJournalpost = JournalføringHelper.lagOppdaterJournalpostRequest(journalpost, eksternFagsakId, dokumenttitler)
        journalpostClient.oppdaterJournalpost(oppdatertJournalpost, journalpost.journalpostId, saksbehandler)
    }

    fun hentSøknadFraJournalpost(søknadJournalpost: Journalpost): Søknadsskjema<SøknadsskjemaBarnetilsyn> {
        val dokumentinfo = JournalføringHelper.plukkUtOriginaldokument(søknadJournalpost, DokumentBrevkode.BARNETILSYN)
        return journalpostClient.hentSøknadTilsynBarn(søknadJournalpost.journalpostId, dokumentinfo.dokumentInfoId)
    }
}
