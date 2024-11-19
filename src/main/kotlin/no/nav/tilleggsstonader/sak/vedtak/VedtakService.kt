package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakTilsynBarn
import org.springframework.data.repository.findByIdOrNull

abstract class VedtakService<DTO>(
    private val stegService: StegService,
    private val steg: BeregnYtelseSteg<DTO>,
    private val repository: VedtakRepository,
) {

    fun håndterSteg(behandlingId: BehandlingId, vedtak: DTO) {
        stegService.håndterSteg(behandlingId, steg, vedtak)
    }

    fun hentVedtak(behandlingId: BehandlingId): VedtakTilsynBarn? {
        return repository.findByIdOrNull(behandlingId)
    }

    fun hentVedtakDto(behandlingId: BehandlingId): DTO? {
        return hentVedtak(behandlingId)?.let(::mapTilDto)
    }

    abstract fun mapTilDto(vedtak: VedtakTilsynBarn): DTO
}
