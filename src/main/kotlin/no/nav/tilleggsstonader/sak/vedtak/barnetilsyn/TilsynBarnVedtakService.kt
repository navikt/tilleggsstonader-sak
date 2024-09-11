package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.BeregningsresultatTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.VedtakTilsynBarnDto
import org.springframework.stereotype.Service

@Service
class TilsynBarnVedtakService(
    repository: TilsynBarnVedtakRepository,
    stegService: StegService,
    tilsynBarnBeregnYtelseSteg: TilsynBarnBeregnYtelseSteg,
) : VedtakService<VedtakTilsynBarnDto, VedtakTilsynBarn>(stegService, tilsynBarnBeregnYtelseSteg, repository) {

    override fun mapTilDto(vedtak: VedtakTilsynBarn): VedtakTilsynBarnDto {
        when (vedtak.type) {
            TypeVedtak.INNVILGELSE -> return InnvilgelseTilsynBarnDto(
                beregningsresultat = vedtak.beregningsresultat?.let {
                    BeregningsresultatTilsynBarnDto(perioder = it.perioder)
                },
            )

            TypeVedtak.AVSLAG -> return AvslagTilsynBarnDto(
                årsakerAvslag = vedtak.årsakerAvslag?.årsaker ?: error("Mangler årsak for avslag"),
                begrunnelse = vedtak.avslagBegrunnelse ?: error("Mangler begrunnelse i avslag"),
            )
        }
    }
}
