package no.nav.tilleggsstonader.sak.brev.frittstående

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.brev.GenererPdfRequest
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Base64

@RestController
@RequestMapping(path = ["/api/frittstaende-brev"])
@ProtectedWithClaims(issuer = "azuread")
class FrittståendeBrevController(
    private val frittståendeBrevService: FrittståendeBrevService,
    private val tilgangService: TilgangService,
) {
    @PostMapping
    fun forhåndsvisFrittsåendeSanitybrev(
        @RequestBody request: GenererPdfRequest,
    ): ByteArray {
        tilgangService.validerHarSaksbehandlerrolle()
        return Base64.getEncoder().encode(frittståendeBrevService.lagFrittståendeSanitybrev(request))
    }

    @PostMapping("/send/{fagsakId}")
    fun sendFrittståendeBrev(
        @PathVariable fagsakId: FagsakId,
        @RequestBody request: FrittståendeBrevDto,
    ) {
        tilgangService.validerHarSaksbehandlerrolle()
        tilgangService.validerTilgangTilFagsak(fagsakId, AuditLoggerEvent.UPDATE)
        frittståendeBrevService.sendFrittståendeBrev(fagsakId, request)
    }
}
