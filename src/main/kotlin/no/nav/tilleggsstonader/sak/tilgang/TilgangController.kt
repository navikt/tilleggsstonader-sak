package no.nav.tilleggsstonader.sak.tilgang

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.opplysninger.egenansatt.EgenAnsattService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/tilgang"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
class TilgangController(
    private val tilgangskontrollService: TilgangskontrollService,
    private val egenAnsattService: EgenAnsattService,
) {
    @GetMapping("/person/{ident}/erEgenAnsatt")
    fun erEgenAnsatt(@PathVariable ident: String): Boolean {
        return egenAnsattService.erEgenAnsatt(ident)
    }

    @GetMapping("/person/{ident}/sjekkTilgangTilPersonMedRelasjoner")
    fun sjekkTilgangTilPersonMedRelasjoner(@PathVariable ident: String): Tilgang {
        return tilgangskontrollService.sjekkTilgangTilPersonMedRelasjoner(
            personIdent = ident,
            jwtToken = SikkerhetContext.hentToken(),
        )
    }
}
