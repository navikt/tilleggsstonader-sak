package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakRequest
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull

abstract class VedtakService<DTO : VedtakRequest>(
    private val stegService: StegService,
    private val steg: BeregnYtelseSteg<DTO>,
    private val repository: VedtakRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun håndterSteg(behandlingId: BehandlingId, vedtak: DTO) {
        logger.info("Lagrer vedtak for behandling=$behandlingId vedtak=${vedtak::class.simpleName}")
        stegService.håndterSteg(behandlingId, steg, vedtak)
    }

    fun hentVedtak(behandlingId: BehandlingId): Vedtak? {
        return repository.findByIdOrNull(behandlingId)
    }
}
