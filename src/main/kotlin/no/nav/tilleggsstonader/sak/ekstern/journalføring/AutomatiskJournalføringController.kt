package no.nav.tilleggsstonader.sak.ekstern.journalføring

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.util.FnrUtil.validerIdent
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    path = ["/api/ekstern/automatisk-journalforing"],
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE],
)
class AutomatiskJournalføringController(private val automatiskJournalføringService: AutomatiskJournalføringService) {

    /**
     * Skal bare brukes av tilleggsstonader-soknad-api for å vurdere om en journalføring skal automatisk ferdigstilles
     * eller manuelt gjennomgås.
     */

    @PostMapping("kan-opprette-behandling")
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun kanOppretteBehandling(
        @RequestBody personIdent: PersonIdent,
        @RequestParam type: Stønadstype,
    ): Boolean {
        if (!SikkerhetContext.kallKommerFraSoknadApi()) {
            throw Feil(message = "Kallet utføres ikke av en autorisert klient", httpStatus = HttpStatus.UNAUTHORIZED)
        }
        validerIdent(personIdent.ident)
        return automatiskJournalføringService.kanOppretteBehandling(personIdent.ident, type)
    }

//    /**
//     * Skal bare brukes av tilleggsstonader-soknad-api for å automatisk journalføre
//     */
//    @PostMapping("journalfor")
//    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
//    fun automatiskJournalfør(@RequestBody request: AutomatiskJournalføringRequest): AutomatiskJournalføringResponse {
//        if (!SikkerhetContext.kallKommerFraSoknadApi()) {
//            throw Feil(message = "Kallet utføres ikke av en autorisert klient", httpStatus = HttpStatus.UNAUTHORIZED)
//        }
//        validerIdent(request.personIdent)
//        return automatiskJournalføringService.automatiskJournalførTilBehandling(
//            journalpostId = request.journalpostId,
//            personIdent = request.personIdent,
//            stønadstype = request.stønadstype,
//            mappeId = request.mappeId,
//            prioritet = request.prioritet,
//        )
//    }
}
