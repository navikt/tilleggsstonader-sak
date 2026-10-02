package no.nav.tilleggsstonader.sak.privatbil

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/forvaltning/kjoreliste")
@ProtectedWithClaims(issuer = "azuread")
class KjørelisteReprosesseringController(
    private val kjørelisteReprosesseringService: KjørelisteReprosesseringService,
    private val tilgangService: TilgangService,
) {
    @GetMapping("/fagsak/{eksternFagsakId}/tilgjengelige")
    fun finnTilgjengeligeKjørelister(
        @PathVariable eksternFagsakId: Long,
    ): List<TilgjengeligKjøreliste> {
        tilgangService.validerHarUtviklerrolle()
        return kjørelisteReprosesseringService.finnTilgjengeligeKjørelister(eksternFagsakId)
    }

    @PostMapping("/{kjørelisteId}/behandle-pa-nytt")
    fun behandlePåNytt(
        @PathVariable kjørelisteId: KjørelisteId,
    ) {
        tilgangService.validerHarUtviklerrolle()
        kjørelisteReprosesseringService.behandlePåNytt(kjørelisteId)
    }
}
