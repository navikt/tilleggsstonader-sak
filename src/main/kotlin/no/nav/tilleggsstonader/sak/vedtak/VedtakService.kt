package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate

abstract class VedtakService<DTO, DOMENE>(
    private val stegService: StegService,
    private val steg: BeregnYtelseSteg<DTO, DOMENE>,
    private val repository: VedtakRepository<DOMENE>,
    private val behandlingService: BehandlingService,
) {

    fun håndterSteg(behandlingId: BehandlingId, vedtak: DTO) {
        stegService.håndterSteg(behandlingId, steg, vedtak)
    }

    fun hentVedtak(behandlingId: BehandlingId): DOMENE? {
        return repository.findByIdOrNull(behandlingId)
    }

    /**
     * Vedtak inneholder perioder før og etter revurderFra, for å kunne lagre et totalbilde på behandlingen.
     * Man ønsker kun å vise perioder fra og med revurderFra på behandling fordi perioder før det datoet ikke er interessant på selve behandlingen
     * Henter kun perioder fra og med revurderFra-datoet
     */
    fun hentVedtakDto(behandlingId: BehandlingId, filtrerFraOgMedRevurderFra: Boolean): DTO? {
        val revurderFra = revurderFraHvisPerioderSkalFiltreresVekk(behandlingId, filtrerFraOgMedRevurderFra)
        return hentVedtak(behandlingId)?.let { mapTilDto(it, revurderFra) }
    }

    /**
     * Henter alle perioder uten å filtrere vekk perioder før revurderFra for å kunne vise et totalbilde
     */
    fun hentVedtakDto(fagsakId: FagsakId): DTO? {
        return behandlingService.finnSisteIverksatteBehandling(fagsakId)
            ?.let { hentVedtakDto(it.id, filtrerFraOgMedRevurderFra = false) }
    }

    private fun revurderFraHvisPerioderSkalFiltreresVekk(
        behandlingId: BehandlingId,
        filtrerFraOgMedRevurderFra: Boolean,
    ): LocalDate? = if (filtrerFraOgMedRevurderFra) {
        behandlingService.hentBehandling(behandlingId).revurderFra
    } else {
        null
    }

    abstract fun mapTilDto(vedtak: DOMENE, revurderFra: LocalDate?): DTO
}
