package no.nav.tilleggsstonader.sak.fagsak

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.fagsak.dto.FagsakDto
import no.nav.tilleggsstonader.sak.fagsak.dto.FagsakRequest
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/fagsak"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FagsakController(private val fagsakService: FagsakService) {

    @PostMapping
    fun hentEllerOpprettFagsakForPerson(@RequestBody fagsakRequest: FagsakRequest): FagsakDto {
        // tilgangService.validerTilgangTilPersonMedBarn(fagsakRequest.personIdent, AuditLoggerEvent.CREATE) // TODO dele opp denne?
        return fagsakService.hentEllerOpprettFagsak(
            fagsakRequest.personIdent,
            fagsakRequest.stønadstype,
        )
    }
}
