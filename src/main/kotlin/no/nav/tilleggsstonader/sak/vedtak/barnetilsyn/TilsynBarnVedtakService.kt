package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TilsynBarnVedtakService(
    repository: TilsynBarnVedtakRepository,
    stegService: StegService,
    tilsynBarnBeregnYtelseSteg: TilsynBarnBeregnYtelseSteg,
) : VedtakService<InnvilgelseTilsynBarnDto>(stegService, tilsynBarnBeregnYtelseSteg, repository) {
    override fun behandlingId(vedtak: InnvilgelseTilsynBarnDto): UUID = vedtak.behandlingId
}
