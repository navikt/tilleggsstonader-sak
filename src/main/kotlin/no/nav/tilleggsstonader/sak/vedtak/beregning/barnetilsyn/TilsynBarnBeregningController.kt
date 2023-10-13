package no.nav.tilleggsstonader.sak.vedtak.beregning.barnetilsyn

import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/beregning/tilsyn-barn")
@ProtectedWithClaims(issuer = "azuread")
class TilsynBarnBeregningController(
    private val tilsynBarnBeregningService: TilsynBarnBeregningService,
) {

    @PostMapping
    fun beregn(vedtak: InnvilgelseTilsynBarnDto): BeregningsresultatTilsynBarnDto {
        return tilsynBarnBeregningService.beregn(vedtak)
    }

    // GET
}
