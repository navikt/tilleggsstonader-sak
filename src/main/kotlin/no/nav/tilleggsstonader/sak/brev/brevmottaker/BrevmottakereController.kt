package no.nav.tilleggsstonader.sak.brev.brevmottaker

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
@RequestMapping(path = ["/api/brevmottakere/"])
@ProtectedWithClaims(issuer = "azuread")
class BrevmottakereController(
    private val tilgangService: TilgangService,
    private val brevmottakereService: BrevmottakereService,
) {

    @GetMapping("/{behandlingId}")
    fun hentBrevmottakere(@PathVariable behandlingId: UUID): BrevmottakereDto {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)

        return brevmottakereService.hentEllerOpprettBrevmottakere(behandlingId)
    }

    @PostMapping("/{behandlingId}")
    fun velgBrevmottakere(
        @PathVariable behandlingId: UUID,
        @RequestBody brevmottakere: BrevmottakereDto,
    ) {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        return brevmottakereService.lagreBrevmottakere(behandlingId, brevmottakere)
    }
}
