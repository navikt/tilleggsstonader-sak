package no.nav.tilleggsstonader.sak.behandling.admin

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.behandling.admin.AdminOpprettBehandlingService.OpprettBehandlingFraJournalpostStatus
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/behandling/journalpost"])
@ProtectedWithClaims(issuer = "azuread")
class AdminOpprettBehandlingController(
    private val adminOpprettBehandlingService: AdminOpprettBehandlingService,
    private val tilgangService: TilgangService,
) {

    @GetMapping("{journalpostId}")
    fun hentStatus(@PathVariable journalpostId: String): OpprettBehandlingFraJournalpostStatus {
        tilgangService.validerHarSaksbehandlerrolle()
        // Tilgangskontroll gjøres inne i opprettBehandlingFraJournalpost
        return adminOpprettBehandlingService.hentInformasjon(journalpostId)
    }

    @PostMapping("{journalpostId}")
    fun opprettBehandlingFraJournalpost(@PathVariable journalpostId: String): UUID {
        tilgangService.validerHarSaksbehandlerrolle()

        // Tilgangskontroll gjøres inne i opprettBehandlingFraJournalpost

        return adminOpprettBehandlingService.opprettBehandlingFraJournalpost(journalpostId)
    }
}
