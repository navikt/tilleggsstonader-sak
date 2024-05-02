package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vedtak.VedtakController
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagRequest
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.BeregningsresultatTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.VedtakTilsynBarnDto
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
    private val tilsynBarnVedtakService: TilsynBarnVedtakService,
) : VedtakController<VedtakTilsynBarnDto, VedtakTilsynBarn>(
    tilgangService,
    tilsynBarnVedtakService,
) {

    @PostMapping("{behandlingId}")
    fun innvilge(
        @PathVariable behandlingId: UUID,
        @RequestBody vedtak: InnvilgelseTilsynBarnRequest,
    ) {
        lagreVedtak(behandlingId, vedtak.tilDto())
    }

    @PostMapping("{behandlingId}/avslag")
    fun avslå(
        @PathVariable behandlingId: UUID,
        @RequestBody vedtak: AvslagRequest,
    ) {
        lagreVedtak(behandlingId, vedtak.tilDto())
    }

    @PostMapping("{behandlingId}/beregn")
    fun beregn(
        @PathVariable behandlingId: UUID,
        @RequestBody vedtak: InnvilgelseTilsynBarnRequest,
    ): BeregningsresultatTilsynBarnDto {
        return tilsynBarnBeregningService.beregn(behandlingId, vedtak.utgifter)
    }
}
