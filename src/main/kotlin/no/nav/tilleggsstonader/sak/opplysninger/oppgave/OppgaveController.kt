package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.oppgave.FinnOppgaveResponseDto
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.dto.FinnOppgaveRequestDto
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.util.FnrUtil.validerOptionalIdent
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/oppgave")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class OppgaveController(
    private val oppgaveClient: OppgaveClient, // erstatt med oppgaveService?
    private val personService: PersonService,
) {

    @PostMapping("/soek")
    fun hentOppgaver(@RequestBody finnOppgaveRequest: FinnOppgaveRequestDto): FinnOppgaveResponseDto {
        validerOptionalIdent(finnOppgaveRequest.ident)

        val aktørId = finnOppgaveRequest.ident.takeUnless { it.isNullOrBlank() }
            ?.let { personService.hentAktørIder(it).identer.first().ident }

        return oppgaveClient.hentOppgaver(finnOppgaveRequest.tilFinnOppgaveRequest(aktørId))
    }

}