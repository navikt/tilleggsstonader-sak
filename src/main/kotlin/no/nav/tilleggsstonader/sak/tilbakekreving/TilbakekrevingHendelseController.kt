package no.nav.tilleggsstonader.sak.tilbakekreving

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.tilbakekreving.dto.TilbakekrevingHendelseDto
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/tilbakekreving"])
@ProtectedWithClaims(issuer = "azuread")
class TilbakekrevingHendelseController(
    private val tilbakekrevinghendelseService: TilbakekrevinghendelseService,
    private val tilgangService: TilgangService,
) {
    @GetMapping("{behandlingId}/hendelser")
    fun hentBehandlingEndretHendelser(
        @PathVariable behandlingId: BehandlingId,
    ): List<TilbakekrevingHendelseDto> {
        tilgangService.settBehandlingsdetaljerForRequest(behandlingId)
        tilgangService.validerLesetilgangTilBehandling(behandlingId)
        return tilbakekrevinghendelseService.hentBehandlingEndretHendelser(behandlingId)
    }
}
