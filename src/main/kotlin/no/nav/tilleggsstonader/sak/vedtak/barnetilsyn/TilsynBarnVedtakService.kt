package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import org.springframework.stereotype.Service

@Service
class TilsynBarnVedtakService(
    repository: TilsynBarnVedtakRepository,
    stegService: StegService,
    tilsynBarnBeregnYtelseSteg: TilsynBarnBeregnYtelseSteg,
) : VedtakService<InnvilgelseTilsynBarnDto, VedtakTilsynBarn>(stegService, tilsynBarnBeregnYtelseSteg, repository) {

    override fun map(vedtak: VedtakTilsynBarn): InnvilgelseTilsynBarnDto {
        return vedtak.vedtak
    }
}
