package no.nav.tilleggsstonader.sak.opplysninger.fullmakt

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.IdentRequest
import no.nav.tilleggsstonader.kontrakter.fullmakt.FullmektigDto
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    path = ["/api/fullmakt"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE],
)
@ProtectedWithClaims(issuer = "azuread")
class FullmaktController(
    private val tilgangService: TilgangService,
    private val fullmaktService: FullmaktService,
) {
    @PostMapping("/fullmektige")
    fun hentFullmektige(@RequestBody fullmaktsgiver: IdentRequest): List<FullmektigDto> {
        tilgangService.validerTilgangTilPerson(fullmaktsgiver.ident, AuditLoggerEvent.ACCESS)

        return fullmaktService.hentFullmektige(fullmaktsgiver.ident)
    }
}
