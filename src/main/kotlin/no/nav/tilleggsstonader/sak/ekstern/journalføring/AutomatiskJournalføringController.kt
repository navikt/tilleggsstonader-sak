package no.nav.tilleggsstonader.sak.ekstern.journalføring

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.sak.journalføring.AutomatiskJournalføringRequest
import no.nav.tilleggsstonader.kontrakter.sak.journalføring.AutomatiskJournalføringResponse
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
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
    fun håndterSøknad(
        @RequestBody request: AutomatiskJournalføringRequest,
    ): AutomatiskJournalføringResponse {
        if (!SikkerhetContext.kallKommerFraSoknadApi()) {
            throw Feil(message = "Kallet utføres ikke av en autorisert klient", httpStatus = HttpStatus.UNAUTHORIZED)
        }
        validerIdent(request.personIdent)
        return automatiskJournalføringService.håndterSøknad(request)
    }
}
