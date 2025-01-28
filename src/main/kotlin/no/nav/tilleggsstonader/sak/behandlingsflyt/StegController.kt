package no.nav.tilleggsstonader.sak.behandlingsflyt

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/steg"])
@ProtectedWithClaims(issuer = "azuread")
class StegController(
    private val stegService: StegService,
    private val tilgangService: TilgangService,
) {
    @PostMapping("behandling/{behandlingId}/ferdigstill")
    fun ferdigstillSteg(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody request: FerdigstillStegRequest,
    ): BehandlingId {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        return stegService.håndterSteg(behandlingId, request.steg).id
    }

    @PostMapping("behandling/{behandlingId}/reset")
    fun resetTilSteg(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody request: ResetStegRequest,
    ) {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        stegService.resetSteg(behandlingId, request.steg)
    }

    data class ResetStegRequest(
        val steg: StegType,
    )

    data class FerdigstillStegRequest(
        val steg: StegType,
    )
}
