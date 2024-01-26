package no.nav.tilleggsstonader.sak.migrering.routing

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.IdentStønadstype
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/migrering/routing"])
@ProtectedWithClaims(issuer = "azuread")
class SøknadRoutingController(
    private val søknadRoutingService: SøknadRoutingService,
) {

    @PostMapping
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun sjekkRoutingForPerson(@RequestBody request: IdentStønadstype): SøknadRoutingResponse {
        return søknadRoutingService.sjekkRoutingForPerson(request)
    }
}
