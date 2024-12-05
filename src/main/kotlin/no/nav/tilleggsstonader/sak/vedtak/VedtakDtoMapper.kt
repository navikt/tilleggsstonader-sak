package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnResponse
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.OpphørTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.VedtakTilsynBarnResponse
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.AvslagLæremidlerDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.VedtakLæremidlerResponse
import java.time.LocalDate

object VedtakDtoMapper {

    fun toDto(vedtak: Vedtak, revurderFra: LocalDate?): VedtakResponse {
        val data = vedtak.data
        return when (data) {
            is VedtakTilsynBarn -> mapVedtakTilsynBarn(data, revurderFra)
            is VedtakLæremidler -> mapVedtakLæremidler(data, revurderFra)
        }
    }

    private fun mapVedtakTilsynBarn(
        data: VedtakTilsynBarn,
        revurderFra: LocalDate?,
    ): VedtakTilsynBarnResponse = when (data) {
        is InnvilgelseTilsynBarn -> InnvilgelseTilsynBarnResponse(
            beregningsresultat = data.beregningsresultat.tilDto(revurderFra = revurderFra),
        )

        is OpphørTilsynBarn -> OpphørTilsynBarnDto(
            årsakerOpphør = data.årsaker,
            begrunnelse = data.begrunnelse,
        )

        is AvslagTilsynBarn -> AvslagTilsynBarnDto(
            årsakerAvslag = data.årsaker,
            begrunnelse = data.begrunnelse,
        )
    }

    private fun mapVedtakLæremidler(data: VedtakLæremidler, revurderFra: LocalDate?): VedtakLæremidlerResponse =
        when (data) {
            is AvslagLæremidler -> AvslagLæremidlerDto(
                årsakerAvslag = data.årsaker,
                begrunnelse = data.begrunnelse,
            )
        }
}
