package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/stonadsperiode"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class StønadsperiodeController(
    private val stønadsperiodeService: StønadsperiodeService,
    private val tilgangService: TilgangService,
) {
    @GetMapping("{behandlingId}")
    fun hentStønadsperioder(
        @PathVariable behandlingId: BehandlingId,
    ): List<StønadsperiodeDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return stønadsperiodeService.hentStønadsperioder(behandlingId)
    }

    @PostMapping("{behandlingId}")
    fun lagreStønadsperioder(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody stønadsperioder: List<StønadsperiodeDto>,
    ): List<StønadsperiodeDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        return stønadsperiodeService.lagreStønadsperioder(behandlingId, stønadsperioder)
    }

    @PostMapping("{behandlingId}/foresla")
    fun preutfyllPerioder(
        @PathVariable behandlingId: BehandlingId,
    ): List<StønadsperiodeDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        tilgangService.validerHarSaksbehandlerrolle()

        return stønadsperiodeService.foreslåPerioder(behandlingId)
    }
}
