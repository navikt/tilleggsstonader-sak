package no.nav.tilleggsstonader.sak.vedlegg

import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VedleggService(
    private val journalpostService: JournalpostService,
    private val fagsakPersonService: FagsakPersonService,
) {
    fun finnVedleggForBruker(fagsakPersonId: UUID, vedleggRequest: VedleggRequest): List<DokumentInfoDto> {
        val aktivIdent = fagsakPersonService.hentAktivIdent(fagsakPersonId)
        val journalposter = journalpostService.finnJournalposterForBruker(aktivIdent, vedleggRequest)

        return journalposter.flatMap { journalpost ->
            journalpost.dokumenter
                ?.map { tilDokumentInfoDto(it, journalpost) }
                ?: emptyList()
        }
    }
}
