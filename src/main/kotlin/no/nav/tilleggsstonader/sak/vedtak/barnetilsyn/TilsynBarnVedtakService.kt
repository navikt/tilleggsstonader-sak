package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.VedtakTilsynBarnRequest
import org.springframework.stereotype.Service

@Service
class TilsynBarnVedtakService(
    repository: VedtakRepository,
    stegService: StegService,
    beregnYtelseSteg: TilsynBarnBeregnYtelseSteg,
) : VedtakService<VedtakTilsynBarnRequest>(stegService, beregnYtelseSteg, repository)
