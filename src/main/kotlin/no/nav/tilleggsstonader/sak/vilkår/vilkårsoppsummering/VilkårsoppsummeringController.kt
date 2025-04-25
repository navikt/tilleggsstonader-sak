package no.nav.tilleggsstonader.sak.vilkår.vilkårsoppsummering

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/vilkarsoppsummering"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VilkårsoppsummeringController(
    private val tilgangService: TilgangService,
    private val vilkårsoppsummeringService: VilkårsoppsummeringService,
) {
    @GetMapping("{behandlingId}")
    fun hentVilkårsoppsummering(
        @PathVariable behandlingId: BehandlingId,
    ): VilkårsoppsummeringDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)

        return vilkårsoppsummeringService.hentVilkårsoppsummering(behandlingId)
    }
}
