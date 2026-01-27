package no.nav.tilleggsstonader.sak.utbetaling.id

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Forvaltning")
@RestController
@RequestMapping("/api/forvaltning/migrer-utbetalinger")
@ProtectedWithClaims(issuer = "azuread")
class FagsakUtbetalingIdMigreringController(
    private val tilgangService: TilgangService,
) {
    @PostMapping
    fun migrerForFagsak() {
        tilgangService.validerHarUtviklerrolle()
        // TODO
    }
}
