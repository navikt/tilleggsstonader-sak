package no.nav.tilleggsstonader.sak.ekstern.stønad

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.IdentStønadstype
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.EksternApplikasjon
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    path = ["/api/ekstern/har-behandling"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE],
)
class HarBehandlingUnderArbeidController(
    private val harBehandlingUnderArbeidService: HarBehandlingUnderArbeidService,
) {
    @PostMapping()
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun harBehandlingUnderArbeid(@RequestBody identStønadstype: IdentStønadstype): Boolean {
        feilHvisIkke(SikkerhetContext.kallKommerFra(EksternApplikasjon.SOKNAD_API), HttpStatus.UNAUTHORIZED) {
            "Kallet utføres ikke av en autorisert klient"
        }
        return harBehandlingUnderArbeidService.harSøknadUnderBehandling(identStønadstype)
    }
}
