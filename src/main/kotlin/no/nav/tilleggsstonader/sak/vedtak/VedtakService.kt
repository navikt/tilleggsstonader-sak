package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import org.springframework.data.repository.findByIdOrNull

abstract class VedtakService<DTO, DOMENE>(
    private val stegService: StegService,
    private val steg: BeregnYtelseSteg<DTO, DOMENE>,
    private val repository: VedtakRepository<DOMENE>,
) {

    fun håndterSteg(behandlingId: BehandlingId, vedtak: DTO) {
        stegService.håndterSteg(behandlingId, steg, vedtak)
    }

    fun hentVedtak(behandlingId: BehandlingId): DOMENE? {
        return repository.findByIdOrNull(behandlingId)
    }

    fun hentVedtakDto(behandlingId: BehandlingId): DTO? {
        return hentVedtak(behandlingId)?.let(::mapTilDto)
    }

    abstract fun mapTilDto(vedtak: DOMENE): DTO
}
