package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.felles.Vedtak
import org.springframework.data.repository.findByIdOrNull

abstract class VedtakService<DOMENE>(
    private val stegService: StegService,
    private val steg: BeregnYtelseSteg<DOMENE>,
) {

    fun håndterSteg(behandlingId: BehandlingId, vedtak: DOMENE) {
        stegService.håndterSteg(behandlingId, steg, vedtak)
    }

    abstract fun hentVedtak(behandlingId: BehandlingId): DOMENE?

}
