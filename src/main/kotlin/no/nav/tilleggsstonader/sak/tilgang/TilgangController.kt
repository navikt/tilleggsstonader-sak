package no.nav.tilleggsstonader.sak.tilgang

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.IdentRequest
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.opplysninger.egenansatt.EgenAnsattService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/tilgang"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
class TilgangController(
    private val tilgangskontrollService: TilgangskontrollService,
    private val egenAnsattService: EgenAnsattService,
) {
    @PostMapping("/person/erEgenAnsatt")
    fun erEgenAnsatt(@RequestBody identRequest: IdentRequest): EgenAnsattResponse {
        return EgenAnsattResponse(erEgenAnsatt = egenAnsattService.erEgenAnsatt(identRequest.ident))
    }

    @PostMapping("person/sjekkTilgangTilPersonMedRelasjoner")
    fun sjekkTilgangTilPersonMedRelasjoner(@RequestBody identRequest: IdentRequest): Tilgang {
        return tilgangskontrollService.sjekkTilgangTilPersonMedRelasjoner(
            personIdent = identRequest.ident,
            jwtToken = SikkerhetContext.hentToken(),
        )
    }
}

data class EgenAnsattResponse(val erEgenAnsatt: Boolean)
