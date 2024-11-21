package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnBeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.VedtakTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakDto
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class VedtakService(
    private val stegService: StegService,
    private val repository: VedtakRepository,
    private val beregnYtelseStegTilsynBarn: TilsynBarnBeregnYtelseSteg,
) {

    fun håndterSteg(behandlingId: BehandlingId, vedtak: VedtakDto) {
        when(vedtak) {
           is VedtakTilsynBarnDto -> stegService.håndterSteg(behandlingId, beregnYtelseStegTilsynBarn, vedtak)
        }
    }

    fun hentVedtak(behandlingId: BehandlingId): Vedtak? {
        return repository.findByIdOrNull(behandlingId)
    }

}