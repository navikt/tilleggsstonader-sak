package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.vedtak.VedtakDtoMapper
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.VedtakTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import org.springframework.stereotype.Service

@Service
class TilsynBarnVedtakService(
    repository: VedtakRepository,
    stegService: StegService,
    tilsynBarnBeregnYtelseSteg: TilsynBarnBeregnYtelseSteg,
    private val behandlingService: BehandlingService,
) : VedtakService<VedtakTilsynBarnDto>(stegService, tilsynBarnBeregnYtelseSteg, repository) {

    override fun mapTilDto(vedtak: Vedtak): VedtakTilsynBarnDto {
        val revurderFra = behandlingService.hentSaksbehandling(vedtak.behandlingId).revurderFra
        return VedtakDtoMapper.toDto(vedtak, revurderFra)
    }
}
