package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.OpphørTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.VedtakTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.tilDto
import org.springframework.stereotype.Service

@Service
class TilsynBarnVedtakService(
    repository: TilsynBarnVedtakRepository,
    stegService: StegService,
    tilsynBarnBeregnYtelseSteg: TilsynBarnBeregnYtelseSteg,
    private val behandlingService: BehandlingService,
) : VedtakService<VedtakTilsynBarnDto, VedtakTilsynBarn>(stegService, tilsynBarnBeregnYtelseSteg, repository) {

    override fun mapTilDto(vedtak: VedtakTilsynBarn): VedtakTilsynBarnDto {
        return when (vedtak.type) {
            TypeVedtak.INNVILGELSE -> {
                val behandling = behandlingService.hentSaksbehandling(vedtak.behandlingId)
                InnvilgelseTilsynBarnDto(
                    beregningsresultat = vedtak.beregningsresultat?.tilDto(revurderFra = behandling.revurderFra),
                )
            }

            TypeVedtak.AVSLAG -> AvslagTilsynBarnDto(
                årsakerAvslag = vedtak.årsakerAvslag?.årsaker ?: error("Mangler årsak for avslag"),
                begrunnelse = vedtak.avslagBegrunnelse ?: error("Mangler begrunnelse i avslag"),
            )

            TypeVedtak.OPPHØR -> OpphørTilsynBarnDto(
                årsakerOpphør = vedtak.årsakerOpphør?.årsaker ?: error("Mangler årsak for opphør"),
                begrunnelse = vedtak.opphørBegrunnelse ?: error("Mangler begrunnelse i opphør"),
            )
        }
    }
}
