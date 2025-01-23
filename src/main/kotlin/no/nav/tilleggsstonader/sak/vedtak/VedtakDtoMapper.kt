package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnResponse
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.OpphørTilsynBarnResponse
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.VedtakTilsynBarnResponse
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.domain.*
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.*
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

        is OpphørTilsynBarn -> OpphørTilsynBarnResponse(
            beregningsresultat = data.beregningsresultat.tilDto(revurderFra = revurderFra),
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
            is InnvilgelseLæremidler -> InnvilgelseLæremidlerResponse(
                vedtaksperioder = data.vedtaksperioder.tilDto(),
                beregningsresultat = data.beregningsresultat.tilDto(),
                gjelderFraOgMed = data.vedtaksperioder.minOf { it.fom },
                gjelderTilOgMed = data.vedtaksperioder.maxOf { it.tom },
            )
            is AvslagLæremidler -> AvslagLæremidlerDto(
                årsakerAvslag = data.årsaker,
                begrunnelse = data.begrunnelse,
            )

            is OpphørLæremidler -> OpphørLæremidlerResponse(
                beregningsresultat = data.beregningsresultat.tilDto(),
                årsakerOpphør = data.årsaker,
                begrunnelse = data.begrunnelse,
            )
        }
}
