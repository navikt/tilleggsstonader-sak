package no.nav.tilleggsstonader.sak.brev.kjørelistebrev

import no.nav.security.token.support.core.api.ProtectedWithClaims
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
@RequestMapping("/api/kjorelistebrev")
@ProtectedWithClaims(issuer = "azuread")
class KjørelisteBehandlingBrevController(
    private val tilgangService: TilgangService,
    private val kjørelisteBehandlingBrevService: KjørelisteBehandlingBrevService,
) {
    @PostMapping("/{behandlingId}")
    fun genererOgLagreBrev(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody genererKjørelistebrevDto: GenererKjørelistebrevDto,
    ): ByteArray {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerSkrivetilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        return Base64.getEncoder().encode(
            kjørelisteBehandlingBrevService.genererOgLagreBrev(behandlingId, genererKjørelistebrevDto).pdf.bytes,
        )
    }

    @GetMapping("/{behandlingId}/begrunnelse")
    fun hentBegrunnelse(
        @PathVariable behandlingId: BehandlingId,
    ): BegrunnelseDto {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerLesetilgangTilBehandling(behandlingId)
        return BegrunnelseDto(begrunnelse = kjørelisteBehandlingBrevService.hentBegrunnelse(behandlingId))
    }

    @GetMapping("/{behandlingId}")
    fun hentBrev(
        @PathVariable behandlingId: BehandlingId,
    ): ByteArray {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerLesetilgangTilBehandling(behandlingId)

        return Base64.getEncoder().encode(kjørelisteBehandlingBrevService.hentBrev(behandlingId).pdf.bytes)
    }
}
