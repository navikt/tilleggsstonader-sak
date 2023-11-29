package no.nav.tilleggsstonader.sak.brev

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
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
}
