package no.nav.tilleggsstonader.sak.brev.brevmottaker

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/brevmottakere/"])
@ProtectedWithClaims(issuer = "azuread")
class BrevmottakereController(
    private val tilgangService: TilgangService,
    private val brevmottakereService: BrevmottakereService,
    private val brevmottakereFrittståendeBrevService: BrevmottakereFrittståendeBrevService,
) {
    @GetMapping("/{behandlingId}")
    fun hentBrevmottakere(
        @PathVariable behandlingId: BehandlingId,
    ): BrevmottakereDto {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)

        return brevmottakereService.hentEllerOpprettBrevmottakere(behandlingId)
    }

    @PostMapping("/{behandlingId}")
    fun velgBrevmottakere(
        @PathVariable behandlingId: BehandlingId,
        @RequestBody brevmottakere: BrevmottakereDto,
    ) {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        return brevmottakereService.lagreBrevmottakere(behandlingId, brevmottakere)
    }

    @GetMapping("fagsak/{fagsakId}")
    fun hentBrevmottakere(
        @PathVariable fagsakId: FagsakId,
    ): BrevmottakereDto {
        tilgangService.validerTilgangTilFagsak(fagsakId, AuditLoggerEvent.ACCESS)

        return brevmottakereFrittståendeBrevService.hentEllerOpprettBrevmottakere(fagsakId).tilBrevmottakereDto()
    }

    @PostMapping("fagsak/{fagsakId}")
    fun velgBrevmottakere(
        @PathVariable fagsakId: FagsakId,
        @RequestBody brevmottakere: BrevmottakereDto,
    ) {
        tilgangService.validerTilgangTilFagsak(fagsakId, AuditLoggerEvent.UPDATE)
        tilgangService.validerHarSaksbehandlerrolle()

        return brevmottakereFrittståendeBrevService.lagreBrevmottakere(fagsakId, brevmottakere)
    }
}
