package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.Simuleringsoppsummering
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/simulering"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class SimuleringController(
    private val tilgangService: TilgangService,
    private val behandlingService: BehandlingService,
    private val simuleringService: SimuleringService,
) {

    @GetMapping("/{behandlingId}")
    fun simulerForBehandling(@PathVariable behandlingId: UUID): Simuleringsoppsummering {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        tilgangService.validerTilgangTilBehandling(saksbehandling, AuditLoggerEvent.UPDATE)
        return simuleringService.simuler(saksbehandling)
    }
}
