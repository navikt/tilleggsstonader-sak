package no.nav.tilleggsstonader.sak.behandlingsflyt

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
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

    @PostMapping("behandling/{behandlingId}/inngangsvilkar")
    fun ferdigstillInngangsvilkår(@PathVariable behandlingId: UUID): UUID {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        return stegService.håndterInngangsvilkår(behandlingId).id
    }
}
