package no.nav.tilleggsstonader.sak.klage

import no.nav.security.token.support.core.api.Unprotected
import no.nav.tilleggsstonader.kontrakter.oppgave.FinnMappeResponseDto
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveResponse
import no.nav.tilleggsstonader.kontrakter.oppgave.OpprettOppgaveRequest
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Implementasjon for å kunne dele på samme oppgaver mellom klage og sak lokalt
 * Klientene i klage og sak er liknende og kan sende videre våre kall til vår mockede [oppgaveClient]
 */
@RestController
@RequestMapping("/test/api/oppgave")
@Unprotected
class OppgaveTestController(
    private val oppgaveClient: OppgaveClient,
) {
    @PostMapping("opprett")
    fun opprettOppgave(
        @RequestBody opprettOppgaveRequest: OpprettOppgaveRequest,
    ): OppgaveResponse = OppgaveResponse(oppgaveClient.opprettOppgave(opprettOppgaveRequest))

    @PatchMapping("{oppgaveId}/ferdigstill")
    fun ferdigstillOppgave(
        @PathVariable oppgaveId: Long,
    ): OppgaveResponse {
        oppgaveClient.ferdigstillOppgave(oppgaveId)
        return OppgaveResponse(oppgaveId)
    }

    @PatchMapping("{oppgaveId}/oppdater")
    fun oppdaterOppgave(
        @RequestBody oppgave: Oppgave,
    ): OppgaveResponse = OppgaveResponse(oppgaveClient.oppdaterOppgave(oppgave).oppgaveId)

    @GetMapping("{oppgaveId}")
    fun finnOppgaveMedId(
        @PathVariable oppgaveId: Long,
    ): Oppgave = oppgaveClient.finnOppgaveMedId(oppgaveId)

    @GetMapping("mappe/sok")
    fun finnMapper(
        @RequestParam enhetsnr: String,
        @RequestParam limit: Int,
    ): FinnMappeResponseDto = oppgaveClient.finnMapper(enhetsnr, limit)
}
