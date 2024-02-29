package no.nav.tilleggsstonader.sak.ekstern.journalføring

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.sak.journalføring.HåndterSøknadRequest
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.EksternApplikasjon
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.util.FnrUtil.validerIdent
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    path = ["/api/ekstern/handter-soknad"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE],
)
class AutomatiskJournalføringController(private val automatiskJournalføringService: AutomatiskJournalføringService) {

    @PostMapping
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun håndterSøknad(@RequestBody request: HåndterSøknadRequest) {
        feilHvisIkke(SikkerhetContext.kallKommerFra(EksternApplikasjon.SOKNAD_API), HttpStatus.UNAUTHORIZED) {
            "Kallet utføres ikke av en autorisert klient"
        }
        validerIdent(request.personIdent)
        automatiskJournalføringService.håndterSøknad(request)
    }
}
