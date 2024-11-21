package no.nav.tilleggsstonader.sak.vedtak

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/vedtak")
@ProtectedWithClaims(issuer = "azuread")
class VedtakController(
    private val tilgangService: TilgangService,
    private val vedtakService: VedtakService,
) {

    /**
     * TODO Post og Get burde kanskje håndtere 2 ulike objekt?
     * På en måte hadde det vært fint hvis GET returnerer beløpsperioder
     */
    /*@GetMapping("{behandlingId}")
    fun hentVedtak(@PathVariable behandlingId: BehandlingId): DTO? {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return vedtakService.hentVedtakDto(behandlingId)
    }

     */
}
