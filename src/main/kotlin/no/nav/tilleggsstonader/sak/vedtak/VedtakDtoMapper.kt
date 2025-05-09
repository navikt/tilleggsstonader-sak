package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.kontrakter.periode.avkortPerioderFør
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnResponse
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.OpphørTilsynBarnResponse
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.VedtakTilsynBarnResponse
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.AvslagBoutgifterDto
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.InnvilgelseBoutgifterResponse
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.OpphørBoutgifterResponse
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.VedtakBoutgifterResponse
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.tilVedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.AvslagLæremidlerDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.InnvilgelseLæremidlerResponse
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.OpphørLæremidlerResponse
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.VedtakLæremidlerResponse
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.tilDto
import java.time.LocalDate

object VedtakDtoMapper {
    fun toDto(
        vedtak: Vedtak,
        revurderFra: LocalDate?,
    ): VedtakResponse {
        val data = vedtak.data
        return when (data) {
            is VedtakTilsynBarn -> mapVedtakTilsynBarn(data, revurderFra)
            is VedtakLæremidler -> mapVedtakLæremidler(data, revurderFra)
            is VedtakBoutgifter -> mapVedtakBoutgifter(data, revurderFra)
        }
    }

    private fun mapVedtakTilsynBarn(
        data: VedtakTilsynBarn,
        revurderFra: LocalDate?,
    ): VedtakTilsynBarnResponse =
        when (data) {
            is InnvilgelseTilsynBarn ->
                InnvilgelseTilsynBarnResponse(
                    beregningsresultat = data.beregningsresultat.tilDto(revurderFra = revurderFra),
                    vedtaksperioder = data.vedtaksperioder?.tilVedtaksperiodeDto(),
                    begrunnelse = data.begrunnelse,
                )

            is OpphørTilsynBarn ->
                OpphørTilsynBarnResponse(
                    beregningsresultat = data.beregningsresultat.tilDto(revurderFra = revurderFra),
                    årsakerOpphør = data.årsaker,
                    begrunnelse = data.begrunnelse,
                    vedtaksperioder = data.vedtaksperioder?.tilVedtaksperiodeDto(),
                )

            is AvslagTilsynBarn ->
                AvslagTilsynBarnDto(
                    årsakerAvslag = data.årsaker,
                    begrunnelse = data.begrunnelse,
                )
        }

    private fun mapVedtakLæremidler(
        data: VedtakLæremidler,
        revurderFra: LocalDate?,
    ): VedtakLæremidlerResponse =
        when (data) {
            is InnvilgelseLæremidler ->
                InnvilgelseLæremidlerResponse(
                    vedtaksperioder = data.vedtaksperioder.tilDto(),
                    beregningsresultat = data.beregningsresultat.tilDto(revurderFra = revurderFra),
                    gjelderFraOgMed = data.vedtaksperioder.avkortPerioderFør(revurderFra).minOf { it.fom },
                    gjelderTilOgMed = data.vedtaksperioder.avkortPerioderFør(revurderFra).maxOf { it.tom },
                    begrunnelse = data.begrunnelse,
                )

            is AvslagLæremidler ->
                AvslagLæremidlerDto(
                    årsakerAvslag = data.årsaker,
                    begrunnelse = data.begrunnelse,
                )

            is OpphørLæremidler ->
                OpphørLæremidlerResponse(
                    årsakerOpphør = data.årsaker,
                    begrunnelse = data.begrunnelse,
                    vedtaksperioder = data.vedtaksperioder.tilDto(),
                )
        }

    private fun mapVedtakBoutgifter(
        data: VedtakBoutgifter,
        revurderFra: LocalDate?,
    ): VedtakBoutgifterResponse =
        when (data) {
            is InnvilgelseBoutgifter ->
                InnvilgelseBoutgifterResponse(
                    vedtaksperioder = data.vedtaksperioder.tilVedtaksperiodeDto(),
                    beregningsresultat = data.beregningsresultat.tilDto(revurderFra = revurderFra),
                    gjelderFraOgMed = data.vedtaksperioder.avkortPerioderFør(revurderFra).minOf { it.fom },
                    gjelderTilOgMed = data.vedtaksperioder.avkortPerioderFør(revurderFra).maxOf { it.tom },
                    begrunnelse = data.begrunnelse,
                )

            is AvslagBoutgifter ->
                AvslagBoutgifterDto(
                    årsakerAvslag = data.årsaker,
                    begrunnelse = data.begrunnelse,
                )

            is OpphørBoutgifter ->
                OpphørBoutgifterResponse(
                    årsakerOpphør = data.årsaker,
                    begrunnelse = data.begrunnelse,
                    vedtaksperioder = data.vedtaksperioder.tilDto(),
                )
        }
}
