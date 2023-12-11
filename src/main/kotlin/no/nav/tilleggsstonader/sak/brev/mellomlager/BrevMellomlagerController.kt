package no.nav.tilleggsstonader.sak.brev.mellomlager

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/brev/mellomlager/"])
@ProtectedWithClaims(issuer = "azuread")
class BrevMellomlagerController(
    private val tilgangService: TilgangService,
    private val mellomlagringBrevService: MellomlagringBrevService,
) {

    @PostMapping("/{behandlingId}")
    fun mellomlagreBrevverdier(
        @PathVariable behandlingId: UUID,
        @RequestBody mellomlagretBrev: MellomlagreBrevDto,
    ): UUID {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        return mellomlagringBrevService.mellomLagreBrev(
            behandlingId,
            mellomlagretBrev.brevverdier,
            mellomlagretBrev.brevmal,
        )
    }

    @GetMapping("/{behandlingId}")
    fun hentMellomlagretBrevverdier(
        @PathVariable behandlingId: UUID,
    ): MellomlagreBrevDto? {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)

        return mellomlagringBrevService.hentMellomlagretBrev(
            behandlingId,
        )
    }

    @PostMapping("/fagsak/{fagsakId}")
    fun mellomlagreFrittståendeSanitybrev(
        @PathVariable fagsakId: UUID,
        @RequestBody mellomlagreBrev: MellomlagreBrevDto,
    ): UUID {
        tilgangService.validerTilgangTilFagsak(fagsakId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        return mellomlagringBrevService.mellomLagreFrittståendeSanitybrev(
            fagsakId,
            mellomlagreBrev.brevverdier,
            mellomlagreBrev.brevmal,
        )
    }

    @GetMapping("/fagsak/{fagsakId}")
    fun hentMellomlagretFrittståendesanitybrev(
        @PathVariable fagsakId: UUID,
    ): MellomlagreBrevDto? {
        tilgangService.validerTilgangTilFagsak(fagsakId, AuditLoggerEvent.ACCESS)

        return mellomlagringBrevService.hentMellomlagretFrittståendeSanitybrev(fagsakId)
    }
}
