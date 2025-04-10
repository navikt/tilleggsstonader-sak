package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.oppgave.MappeDto
import no.nav.tilleggsstonader.sak.fagsak.domain.FagsakPersonService
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveUtil.ENHET_NR_EGEN_ANSATT
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveUtil.ENHET_NR_NAY
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveUtil.ENHET_NR_STRENGT_FORTROLIG
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.dto.FinnOppgaveRequestDto
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.dto.FinnOppgaveResponseDto
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.dto.OppgaveDto
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.dto.tilDto
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/oppgave")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class OppgaveController(
    private val oppgaveService: OppgaveService,
    private val tilgangService: TilgangService,
    private val fagsakPersonService: FagsakPersonService,
) {
    @PostMapping("/soek")
    fun hentOppgaver(
        @RequestBody finnOppgaveRequest: FinnOppgaveRequestDto,
    ): FinnOppgaveResponseDto = oppgaveService.hentOppgaver(finnOppgaveRequest).tilDto()

    @GetMapping("/soek/person/{fagsakPersonId}")
    fun hentOppgaverForPerson(
        @PathVariable fagsakPersonId: FagsakPersonId,
    ): FinnOppgaveResponseDto {
        val personIdent = fagsakPersonService.hentAktivIdent(fagsakPersonId)
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)

        return oppgaveService.hentOppgaverForPerson(personIdent).tilDto()
    }

    @PostMapping(path = ["/{oppgaveId}/fordel"])
    fun tildelOppgave(
        @PathVariable(name = "oppgaveId") oppgaveId: Long,
        @RequestParam("versjon") versjon: Int,
        @RequestParam("tilbakestill") tilbakestill: Boolean,
    ): OppgaveDto {
        tilgangService.validerHarSaksbehandlerrolle()
        val tildeltSaksbehandler = if (tilbakestill) null else SikkerhetContext.hentSaksbehandler()
        return oppgaveService.fordelOppgave(oppgaveId, tildeltSaksbehandler, versjon).tilDto()
    }

    @GetMapping("/mapper")
    fun hentMapper(): List<MappeDto> {
        val enheter = mutableListOf(ENHET_NR_NAY)
        if (tilgangService.harEgenAnsattRolle()) {
            enheter += ENHET_NR_EGEN_ANSATT
        }
        if (tilgangService.harStrengtFortroligRolle()) {
            enheter += ENHET_NR_STRENGT_FORTROLIG
        }
        return oppgaveService.finnMapper(enheter = enheter)
    }
}
