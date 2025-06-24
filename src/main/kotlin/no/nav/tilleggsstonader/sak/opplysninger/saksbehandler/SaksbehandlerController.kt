package no.nav.tilleggsstonader.sak.opplysninger.saksbehandler

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.dto.SaksbehandlerDto
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/saksbehandler")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class SaksbehandlerController(
    private val saksbehandlerService: SaksbehandlerService,
) {
    @GetMapping("/{behandlingId}")
    fun hentTilordnetSaksbehandler(
        @PathVariable behandlingId: BehandlingId,
    ): SaksbehandlerDto? = saksbehandlerService.finnSaksbehandler(behandlingId)
}
