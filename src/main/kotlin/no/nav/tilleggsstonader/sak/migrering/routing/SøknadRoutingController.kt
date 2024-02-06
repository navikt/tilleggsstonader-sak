package no.nav.tilleggsstonader.sak.migrering.routing

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.IdentStønadstype
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.EksternApplikasjon
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/ekstern/routing-soknad"])
@ProtectedWithClaims(issuer = "azuread")
class SøknadRoutingController(
    private val søknadRoutingService: SøknadRoutingService,
) {

    @PostMapping
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun sjekkRoutingForPerson(@RequestBody request: IdentStønadstype): SøknadRoutingResponse {
        feilHvisIkke(SikkerhetContext.kallKommerFra(EksternApplikasjon.SOKNAD_API), HttpStatus.UNAUTHORIZED) {
            "Kallet utføres ikke av en autorisert klient"
        }
        return søknadRoutingService.sjekkRoutingForPerson(request)
    }
}
