package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.felles.VedtakTilsynBarn
import org.springframework.stereotype.Service

@Service
class TilsynBarnVedtakService(
    private val repository: TilsynBarnVedtakRepository,
    stegService: StegService,
    tilsynBarnBeregnYtelseSteg: TilsynBarnBeregnYtelseSteg,
    private val behandlingService: BehandlingService,
) : VedtakService<VedtakTilsynBarn>(stegService, tilsynBarnBeregnYtelseSteg) {
    override fun hentVedtak(behandlingId: BehandlingId): VedtakTilsynBarn? {
        TODO("Not yet implemented")
    }


}
