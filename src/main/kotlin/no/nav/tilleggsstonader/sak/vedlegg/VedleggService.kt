package no.nav.tilleggsstonader.sak.vedlegg

import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import org.springframework.stereotype.Service

@Service
class VedleggService(
    private val journalpostService: JournalpostService,
    private val fagsakPersonService: FagsakPersonService,
) {
    fun finnVedleggForBruker(
        fagsakPersonId: FagsakPersonId,
        vedleggRequest: VedleggRequest,
    ): List<DokumentInfoDto> {
        val aktivIdent = fagsakPersonService.hentAktivIdent(fagsakPersonId)
        val journalposter = journalpostService.finnJournalposterForBruker(aktivIdent, vedleggRequest)

        return journalposter.flatMap { journalpost ->
            journalpost.dokumenter
                ?.filter { it.brevkode != BrevkodeVedlegg.INNSENDINGSKVITTERING.name }
                ?.map { tilDokumentInfoDto(it, journalpost) }
                ?: emptyList()
        }
    }
}
