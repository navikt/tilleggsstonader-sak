package no.nav.tilleggsstonader.sak.behandling.historikk

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.historikk.dto.HendelseshistorikkDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/behandlingshistorikk"])
@ProtectedWithClaims(issuer = "azuread")
class BehandlingshistorikkController(
    private val behandlingshistorikkService: BehandlingshistorikkService,
    // private val tilgangService: TilgangService,
    private val behandlingService: BehandlingService,
) {

    @GetMapping("{behandlingId}")
    fun hentBehandlingshistorikk(@PathVariable behandlingId: UUID): List<HendelseshistorikkDto> {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        // tilgangService.validerTilgangTilBehandling(saksbehandling, AuditLoggerEvent.ACCESS)
        val behandlingHistorikk = behandlingshistorikkService.finnHendelseshistorikk(saksbehandling)
        return behandlingHistorikk
    }
}
