package no.nav.tilleggsstonader.sak.privatbil.varsel

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Forvaltning")
@RestController
@RequestMapping("/api/forvaltning/kjorelistevarsel")
@ProtectedWithClaims(issuer = "azuread")
@Profile("!prod")
class KjørelisteVarselController(
    private val tilgangService: TilgangService,
    private val kjørelistevarselService: KjørelistevarselService,
) {
    @GetMapping("/send")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun sendUkentligVarselOmKjørelister() {
        tilgangService.validerHarUtviklerrolle()
        kjørelistevarselService.sendUkentligVarselOmKjørelister()
    }
}
