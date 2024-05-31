package no.nav.tilleggsstonader.sak.klage

import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.klage.dto.KlagebehandlingerDto
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class KlageService(private val fagsakService: FagsakService) {
    fun hentBehandlinger(fagsakPersonId: UUID): KlagebehandlingerDto {
        val fagsaker = fagsakService.finnFagsakerForFagsakPersonId(fagsakPersonId)
        val eksterneFagsakIder = listOfNotNull(fagsaker.barnetilsyn)

        if (eksterneFagsakIder.isEmpty()) {
            return KlagebehandlingerDto(emptyList(), emptyList(), emptyList())
        }

        TODO()
    }
}
