package no.nav.tilleggsstonader.sak.vedtak.læremidler.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import java.time.LocalDate

data class InnvilgelseLæremidlerResponse(
    val vedtaksperioder: List<VedtaksperiodeDto>,
    val beregningsresultat: BeregningsresultatLæremidlerDto,
    val gjelderFraOgMed: LocalDate,
    val gjelderTilOgMed: LocalDate,
) : VedtakLæremidlerResponse, VedtakLæremidlerDto(TypeVedtak.INNVILGELSE)

data class InnvilgelseLæremidlerRequest(
    val vedtaksperioder: List<VedtaksperiodeDto>,
) : VedtakLæremidlerRequest, VedtakLæremidlerDto(TypeVedtak.INNVILGELSE)
