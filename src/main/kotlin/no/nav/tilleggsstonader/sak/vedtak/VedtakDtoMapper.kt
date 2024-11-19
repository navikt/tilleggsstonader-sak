package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.OpphørTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.VedtakTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import java.time.LocalDate

object VedtakDtoMapper {

    fun toDto(vedtak: Vedtak, revurderFra: LocalDate?): VedtakTilsynBarnDto {
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

            TypeVedtak.OPPHØR -> OpphørTilsynBarnDto(
                årsakerOpphør = vedtak.årsakerOpphør?.årsaker ?: error("Mangler årsak for opphør"),
                begrunnelse = vedtak.opphørBegrunnelse ?: error("Mangler begrunnelse i opphør"),
            )
        }
    }
}
