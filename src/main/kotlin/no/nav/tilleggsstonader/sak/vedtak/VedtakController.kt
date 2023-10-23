package no.nav.tilleggsstonader.sak.vedtak

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.util.UUID

@ProtectedWithClaims(issuer = "azuread")
abstract class VedtakController<DTO, DOMENE>(
    private val tilgangService: TilgangService,
    private val vedtakService: VedtakService<DTO, DOMENE>,
) {
    @PostMapping("{behandlingId}")
    fun lagreVedtak(@PathVariable behandlingId: UUID, @RequestBody vedtak: DTO) {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.CREATE)
        vedtakService.håndterSteg(behandlingId, vedtak)
    }

    /**
     * TODO Post og Get burde kanskje håndtere 2 ulike objekt?
     * På en måte hadde det vært fint hvis GET returnerer beløpsperioder
     */
    @GetMapping("{behandlingId}")
    fun hentVedtak(@PathVariable behandlingId: UUID): DTO? {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return vedtakService.hentVedtakDto(behandlingId)
    }
}
