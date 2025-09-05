package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/forvaltning/oppgave")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class GjenopprettOppgaveController(
    private val tilgangService: TilgangService,
) {
    @GetMapping("/gjenopprett")
    fun gjenopprettOppgave(): Any {
        tilgangService.validerHarUtviklerrolle()
        return mapOf("status" to "TODO")
    }
}
