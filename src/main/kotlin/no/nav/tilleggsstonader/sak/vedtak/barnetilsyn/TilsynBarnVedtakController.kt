package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vedtak.VedtakController
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/vedtak/tilsyn-barn")
class TilsynBarnVedtakController(
    private val tilsynBarnBeregningService: TilsynBarnBeregningService,
    tilgangService: TilgangService,
    tilsynBarnVedtakService: TilsynBarnVedtakService,
) : VedtakController<InnvilgelseTilsynBarnDto, VedtakTilsynBarn>(
    tilgangService,
    tilsynBarnVedtakService,
) {

    @PostMapping("{behandlingId}/beregn")
    fun beregn(
        @PathVariable behandlingId: UUID,
        @RequestBody vedtak: InnvilgelseTilsynBarnDto,
    ): BeregningsresultatTilsynBarnDto {
        return tilsynBarnBeregningService.beregn(behandlingId, vedtak.utgifter)
    }
}
