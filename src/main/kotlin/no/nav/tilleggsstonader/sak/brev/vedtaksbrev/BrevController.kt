package no.nav.tilleggsstonader.sak.brev.vedtaksbrev

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.brev.GenererPdfRequest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Base64

@RestController
@RequestMapping("/api/brev")
@ProtectedWithClaims(issuer = "azuread")
class BrevController(
    private val tilgangService: TilgangService,
    private val behandlingService: BehandlingService,
    private val brevService: BrevService,
) {
    @PostMapping("/{behandlingId}")
    fun genererPdf(
        @RequestBody request: GenererPdfRequest,
        @PathVariable behandlingId: BehandlingId,
    ): ByteArray {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)

        tilgangService.validerTilgangTilBehandling(saksbehandling.id, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        return Base64.getEncoder().encode(brevService.lagSaksbehandlerBrev(saksbehandling, request.html))
    }

    @GetMapping("/{behandlingId}")
    fun hentBrev(
        @PathVariable behandlingId: BehandlingId,
    ): ByteArray {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)

        return Base64.getEncoder().encode(brevService.hentBeslutterbrevEllerRekonstruerSaksbehandlerBrev(behandlingId))
    }

    @GetMapping("/beslutter/{behandlingId}")
    fun forhåndsvisBeslutterbrev(
        @PathVariable behandlingId: BehandlingId,
    ): ByteArray {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        tilgangService.validerTilgangTilBehandling(saksbehandling.id, AuditLoggerEvent.ACCESS)

        return Base64.getEncoder().encode(brevService.forhåndsvisBeslutterBrev(saksbehandling))
    }
}
