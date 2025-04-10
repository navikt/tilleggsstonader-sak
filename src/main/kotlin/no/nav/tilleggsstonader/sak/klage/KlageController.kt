package no.nav.tilleggsstonader.sak.klage

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.klage.FagsystemVedtak
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.klage.dto.KlagebehandlingerDto
import no.nav.tilleggsstonader.sak.klage.dto.OpprettKlageDto
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/klage"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
class KlageController(
    private val tilgangService: TilgangService,
    private val klageService: KlageService,
    private val eksternVedtakService: EksternKlageVedtakService,
) {
    /**
     * Brukes av "Opprett behandling"-modalen i sak-frontend
     */
    @PostMapping("/fagsak/{fagsakId}")
    fun opprettKlage(
        @PathVariable fagsakId: FagsakId,
        @RequestBody opprettKlageDto: OpprettKlageDto,
    ): FagsakId {
        tilgangService.validerTilgangTilFagsak(fagsakId, AuditLoggerEvent.CREATE)
        tilgangService.validerHarSaksbehandlerrolle()
        klageService.opprettKlage(fagsakId, opprettKlageDto)
        return fagsakId
    }

    /**
     * Brukes av sak-frontend for å hente klagebehandlinger som listes opp i personoversikten
     */
    @GetMapping("/fagsak-person/{fagsakPersonId}")
    fun hentKlagebehandlinger(
        @PathVariable fagsakPersonId: FagsakPersonId,
    ): KlagebehandlingerDto {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        return klageService.hentBehandlinger(fagsakPersonId)
    }

    /**
     * Kalles på av klage-backend for å populere listen over vedtak som det kan klages på
     */
    @GetMapping(path = ["/ekstern-fagsak/{eksternFagsakId}/vedtak"])
    @ProtectedWithClaims(issuer = "azuread")
    fun hentVedtak(
        @PathVariable eksternFagsakId: Long,
    ): List<FagsystemVedtak> {
        if (!SikkerhetContext.erMaskinTilMaskinToken()) {
            tilgangService.validerTilgangTilEksternFagsak(eksternFagsakId, AuditLoggerEvent.ACCESS)
        }

        return eksternVedtakService.hentVedtak(eksternFagsakId)
    }
}
