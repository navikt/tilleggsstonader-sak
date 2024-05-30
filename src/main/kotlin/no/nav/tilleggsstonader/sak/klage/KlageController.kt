package no.nav.tilleggsstonader.sak.klage

import no.nav.familie.prosessering.rest.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.kontrakter.klage.FagsystemVedtak
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.klage.dto.KlagebehandlingerDto
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/klage"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
class KlageController(
    private val tilgangService: TilgangService,
    private val klageService: KlageService,
    private val eksternVedtakService: EksternVedtakService,
) {
    @GetMapping("/fagsak/{fagsakPersonId}")
    fun hentKlagebehandlinger(@PathVariable fagsakPersonId: UUID): Ressurs<KlagebehandlingerDto> {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        return Ressurs.success(klageService.hentBehandlinger(fagsakPersonId))
    }

    @GetMapping("/ekstern-fagsak/{eksternFagsakId}/vedtak")
    @ProtectedWithClaims(issuer = "azuread")
    fun hentVedtak(@PathVariable eksternFagsakId: Long): Ressurs<List<FagsystemVedtak>> {
        if (!SikkerhetContext.erMaskinTilMaskinToken()) {
            tilgangService.validerTilgangTilEksternFagsak(eksternFagsakId, AuditLoggerEvent.ACCESS)
        }

        return Ressurs.success(eksternVedtakService.hentVedtak(eksternFagsakId))
    }
}
