package no.nav.tilleggsstonader.sak.behandlingsflyt

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/steg"])
@ProtectedWithClaims(issuer = "azuread")
class StegController(
    private val stegService: StegService,
    private val tilgangService: TilgangService,
) {

    @PostMapping("behandling/{behandlingId}/ferdigstill")
    fun ferdigstillSteg(@PathVariable behandlingId: UUID, @RequestBody request: FerdigstillStegRequest): UUID {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        return when (request.steg) {
            StegType.INNGANGSVILKÅR -> stegService.håndterInngangsvilkår(behandlingId).id
            StegType.VILKÅR -> stegService.håndterVilkår(behandlingId).id
            else -> error("Steg $request.steg kan ikke ferdigstilles her")
        }
    }

    @PostMapping("behandling/{behandlingId}/reset")
    fun resetTilSteg(@PathVariable behandlingId: UUID, @RequestBody request: ResetStegRequest) {
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
