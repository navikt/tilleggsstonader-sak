package no.nav.tilleggsstonader.sak.vedtak.læremidler.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import java.time.LocalDate

data class OpphørLæremidlerResponse(
    val vedtaksperioder: List<VedtaksperiodeDto>,
    val beregningsresultat: BeregningsresultatLæremidlerDto,
    val gjelderFraOgMed: LocalDate,
    val gjelderTilOgMed: LocalDate,
) : VedtakLæremidlerResponse, VedtakLæremidlerDto(TypeVedtak.OPPHØR)

data class OpphørLæremidlerRequest(
    val vedtaksperioder: List<VedtaksperiodeDto>,
) : VedtakLæremidlerRequest, VedtakLæremidlerDto(TypeVedtak.OPPHØR)
