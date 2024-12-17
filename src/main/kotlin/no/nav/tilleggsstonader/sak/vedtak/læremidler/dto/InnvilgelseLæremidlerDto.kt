package no.nav.tilleggsstonader.sak.vedtak.læremidler.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
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

data class VedtaksperiodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
)

fun List<Vedtaksperiode>.tilDto() = this.map { VedtaksperiodeDto(fom = it.fom, tom = it.tom) }

fun List<VedtaksperiodeDto>.tilDomene() = this.map { Vedtaksperiode(fom = it.fom, tom = it.tom) }
