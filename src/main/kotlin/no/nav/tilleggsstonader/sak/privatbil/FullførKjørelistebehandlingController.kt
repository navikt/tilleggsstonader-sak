package no.nav.tilleggsstonader.sak.privatbil

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/kjoreliste"])
@ProtectedWithClaims(issuer = "azuread")
class FullførKjørelistebehandlingController(
    private val tilgangService: TilgangService,
    private val stegService: StegService,
    private val fullførKjørelisteBehandlingSteg: FullførKjørelistebehandlingSteg,
) {
    @PostMapping("/{behandlingId}/fullfør")
    fun fullførKjørelisteBehandling(
        @PathVariable behandlingId: BehandlingId,
    ) {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerSkrivetilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        stegService.håndterSteg(behandlingId, fullførKjørelisteBehandlingSteg)
    }
}
