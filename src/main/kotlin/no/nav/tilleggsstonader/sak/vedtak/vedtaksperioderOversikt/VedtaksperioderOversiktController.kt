package no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakPersonId
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vedtak.domain.DetaljertVedtaksperiode
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/vedtak")
@ProtectedWithClaims(issuer = "azuread")
class VedtaksperioderOversiktController(
    private val vedtakOversiktService: VedtaksperioderOversiktService,
    private val tilgangService: TilgangService,
) {
    @GetMapping("/fullstendig-oversikt/{fagsakPersonId}")
    fun hentFullstendigVedtaksoversikt(
        @PathVariable fagsakPersonId: FagsakPersonId,
    ): VedtaksperioderOversikt {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        return vedtakOversiktService.hentVedtaksperioderOversikt(fagsakPersonId)
    }

    @GetMapping("/detaljerte-vedtaksperioder/{behandlingId}")
    fun hentDetaljerteVedtaksperioderForBehandling(
        @PathVariable behandlingId: BehandlingId,
    ): List<DetaljertVedtaksperiode> {
        tilgangService.validerTilgangTilBehandling(behandlingId, AuditLoggerEvent.ACCESS)
        return vedtakOversiktService.hentDetaljerteVedtaksperioderForBehandling(behandlingId)
    }
}
