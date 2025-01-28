package no.nav.tilleggsstonader.sak.behandling.admin

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/behandling/admin"])
@ProtectedWithClaims(issuer = "azuread")
class AdminOpprettBehandlingController(
    private val adminOpprettBehandlingService: AdminOpprettBehandlingService,
    private val tilgangService: TilgangService,
) {
    @PostMapping("hent-person")
    fun hentPerson(
        @RequestBody request: AdminOpprettFørstegangsbehandlingHentPersonDto,
    ): PersoninfoDto {
        tilgangService.validerHarSaksbehandlerrolle()
        tilgangService.validerTilgangTilPersonMedBarn(request.ident, AuditLoggerEvent.ACCESS)

        return adminOpprettBehandlingService.hentPerson(stønadstype = request.stønadstype, ident = request.ident)
    }

    @PostMapping("opprett-foerstegangsbehandling")
    fun opprettFørstegangsbehandling(
        @RequestBody request: AdminOpprettFørstegangsbehandlingDto,
    ): BehandlingId {
        tilgangService.validerHarSaksbehandlerrolle()
        tilgangService.validerTilgangTilPersonMedBarn(request.ident, AuditLoggerEvent.CREATE)

        return adminOpprettBehandlingService.opprettFørstegangsbehandling(
            stønadstype = request.stønadstype,
            ident = request.ident,
            valgteBarn = request.valgteBarn,
            medBrev = request.medBrev,
        )
    }
}
