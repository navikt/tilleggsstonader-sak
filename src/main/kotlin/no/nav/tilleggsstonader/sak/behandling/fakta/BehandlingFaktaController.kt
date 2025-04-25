package no.nav.tilleggsstonader.sak.behandling.fakta

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/behandling"])
@ProtectedWithClaims(issuer = "azuread")
class BehandlingFaktaController(
    private val tilgangService: TilgangService,
    private val behandlingFaktaService: BehandlingFaktaService,
) {
    @GetMapping("{behandlingId}/fakta")
    fun hentBehandlingFakta(
        @PathVariable behandlingId: BehandlingId,
    ): BehandlingFaktaDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return behandlingFaktaService.hentFakta(behandlingId)
    }
}
