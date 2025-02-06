package no.nav.tilleggsstonader.sak.vedtak.læremidler.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import java.time.LocalDate

data class InnvilgelseLæremidlerResponse(
    val vedtaksperioder: List<VedtaksperiodeLæremidlerDto>,
    val beregningsresultat: BeregningsresultatLæremidlerDto,
    val gjelderFraOgMed: LocalDate,
    val gjelderTilOgMed: LocalDate,
) : VedtakLæremidlerDto(TypeVedtak.INNVILGELSE),
    VedtakLæremidlerResponse

data class InnvilgelseLæremidlerRequest(
    val vedtaksperioder: List<VedtaksperiodeLæremidlerDto>,
) : VedtakLæremidlerDto(TypeVedtak.INNVILGELSE),
    VedtakLæremidlerRequest
