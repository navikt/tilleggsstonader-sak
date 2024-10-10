package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.VedtakTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.tilDto
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class TilsynBarnVedtakService(
    repository: TilsynBarnVedtakRepository,
    stegService: StegService,
    tilsynBarnBeregnYtelseSteg: TilsynBarnBeregnYtelseSteg,
    behandlingService: BehandlingService,
) : VedtakService<VedtakTilsynBarnDto, VedtakTilsynBarn>(
    stegService,
    tilsynBarnBeregnYtelseSteg,
    repository,
    behandlingService,
) {

    override fun mapTilDto(vedtak: VedtakTilsynBarn, revurderFra: LocalDate?): VedtakTilsynBarnDto {
        return when (vedtak.type) {
            TypeVedtak.INNVILGELSE -> {
                InnvilgelseTilsynBarnDto(
                    beregningsresultat = vedtak.beregningsresultat?.tilDto(revurderFra = revurderFra),
                )
            }

            TypeVedtak.AVSLAG -> AvslagTilsynBarnDto(
                årsakerAvslag = vedtak.årsakerAvslag?.årsaker ?: error("Mangler årsak for avslag"),
                begrunnelse = vedtak.avslagBegrunnelse ?: error("Mangler begrunnelse i avslag"),
            )
        }
    }
}
