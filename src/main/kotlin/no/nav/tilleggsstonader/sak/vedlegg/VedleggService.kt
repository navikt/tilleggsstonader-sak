package no.nav.tilleggsstonader.sak.vedlegg

import no.nav.tilleggsstonader.kontrakter.journalpost.Journalpost
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import org.springframework.stereotype.Service

@Service
class VedleggService(
    private val journalpostService: JournalpostService,
    private val fagsakPersonService: FagsakPersonService
) {
    fun finnVedleggForBruker(vedleggRequest: VedleggRequest): List<Journalpost> {
        val aktivIdent = fagsakPersonService.hentAktivIdent(vedleggRequest.fagsakPersonId)
        return journalpostService.finnJournalposterForBruker(aktivIdent, vedleggRequest)
    }
}
