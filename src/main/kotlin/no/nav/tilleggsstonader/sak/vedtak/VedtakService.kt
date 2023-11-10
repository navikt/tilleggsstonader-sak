package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import org.springframework.data.repository.findByIdOrNull
import java.util.UUID

abstract class VedtakService<DTO, DOMENE>(
    private val stegService: StegService,
    private val steg: BeregnYtelseSteg<DTO, DOMENE>,
    private val repository: VedtakRepository<DOMENE>,
) {

    fun håndterSteg(behandlingId: UUID, vedtak: DTO) {
        stegService.håndterSteg(behandlingId, steg, vedtak)
    }

    fun hentVedtak(behandlingId: UUID): DOMENE? {
        return repository.findByIdOrNull(behandlingId)
    }

    fun hentVedtakDto(behandlingId: UUID): DTO? {
        return hentVedtak(behandlingId)?.let(::mapTilDto)
    }

    abstract fun mapTilDto(vedtak: DOMENE): DTO
}
