package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnVedtakRepository
import java.util.UUID

abstract class VedtakService<T>(
    private val stegService: StegService,
    private val steg: BeregnYtelseSteg<T>,
    private val tilsynBarnVedtakRepository: TilsynBarnVedtakRepository,
) {

    fun håndterSteg(behandlingId: UUID, vedtak: T) {
        stegService.håndterSteg(behandlingId, steg, vedtak)
    }

    fun hentVedtak(behandlingId: UUID): T {
        // TODO erstatt med riktig repo når TilsynBarnVedtakRepository er et riktig repo med interface
        return tilsynBarnVedtakRepository.findByIdOrNull(behandlingId) as T
    }
}
