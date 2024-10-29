package no.nav.tilleggsstonader.sak.behandling.admin

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.felles.IdentRequest
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
    fun hentPerson(@RequestBody identRequest: IdentRequest): PersoninfoDto {
        tilgangService.validerHarSaksbehandlerrolle()
        tilgangService.validerTilgangTilPersonMedBarn(identRequest.ident, AuditLoggerEvent.ACCESS)

        return adminOpprettBehandlingService.hentPerson(ident = identRequest.ident)
    }

    @PostMapping("opprett-foerstegangsbehandling")
    fun opprettFørstegangsbehandling(@RequestBody request: AdminOpprettFørstegangsbehandlingDto): BehandlingId {
        tilgangService.validerHarSaksbehandlerrolle()
        tilgangService.validerTilgangTilPersonMedBarn(request.ident, AuditLoggerEvent.CREATE)

        return adminOpprettBehandlingService.opprettFørstegangsbehandling(
            ident = request.ident,
            valgteBarn = request.valgteBarn,
            medBrev = request.medBrev,
        )
    }
}
