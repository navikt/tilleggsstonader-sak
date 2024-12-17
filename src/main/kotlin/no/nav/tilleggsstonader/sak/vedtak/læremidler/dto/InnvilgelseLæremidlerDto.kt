package no.nav.tilleggsstonader.sak.vedtak.læremidler.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode

data class InnvilgelseLæremidlerResponse(
    val vedtaksperioder: List<Vedtaksperiode>,
    val beregningsresultat: BeregningsresultatLæremidlerDto,
) : VedtakLæremidlerResponse, VedtakLæremidlerDto(TypeVedtak.INNVILGELSE)

data class InnvilgelseLæremidlerRequest(
    val vedtaksperioder: List<Vedtaksperiode>,
) : VedtakLæremidlerRequest, VedtakLæremidlerDto(TypeVedtak.INNVILGELSE)
