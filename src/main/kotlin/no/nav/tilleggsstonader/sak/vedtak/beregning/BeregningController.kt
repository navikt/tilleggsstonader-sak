package no.nav.tilleggsstonader.sak.vedtak.beregning

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.vedtak.beregning.barnetilsyn.BeregningsresultatTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.beregning.barnetilsyn.InnvilgelseTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.beregning.barnetilsyn.TilsynBarnBeregningService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/beregning")
@ProtectedWithClaims(issuer = "azuread")
class BeregningController(
    private val tilsynBarnBeregningService: TilsynBarnBeregningService,
) {

    @PostMapping("tilsyn-barn")
    fun beregn(vedtak: InnvilgelseTilsynBarnDto): BeregningsresultatTilsynBarnDto {
        return tilsynBarnBeregningService.beregn(vedtak)
    }
}
