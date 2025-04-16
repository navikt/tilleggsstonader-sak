package no.nav.tilleggsstonader.sak.vilkår.vilkårsoppsummering

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.behandling.oppsummering.BehandlingOppsummeringDto
import no.nav.tilleggsstonader.sak.behandling.oppsummering.BehandlingOppsummeringService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/behandlingsoppsummering"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class BehandlingOppsummeringController(
    private val tilgangService: TilgangService,
    private val behandlingOppsummeringService: BehandlingOppsummeringService,
) {
    @GetMapping("{behandlingId}")
    fun hentVilkårsoppsummering(
        @PathVariable behandlingId: BehandlingId,
    ): BehandlingOppsummeringDto {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)

        return behandlingOppsummeringService.hentBehandlingOppsummering(behandlingId)
    }
}
