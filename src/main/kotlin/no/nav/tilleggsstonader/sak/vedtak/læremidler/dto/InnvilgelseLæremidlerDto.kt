package no.nav.tilleggsstonader.sak.vedtak.læremidler.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dto.LagretVedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import java.time.LocalDate

/**
 * @param vedtaksperioder viser alle vedtaksperioder, inkl vedtaksperioder før revurder-fra dato
 * @param beregningsresultat viser beregningsresultatet fra og med revurder-fra-dato
 * Beregningsresultatet kan være en del av en løpende måned. Eks 1jan-31jan med revurder-fra 15 gir 15jan-31jan
 * @param gjelderFraOgMed gjelder fra og med revurder-fra-dato, brukes til brevet for å vise fra når vedtaket gjelder fra
 */
data class InnvilgelseLæremidlerResponse(
    val vedtaksperioder: List<LagretVedtaksperiodeDto>,
    val beregningsresultat: BeregningsresultatLæremidlerDto,
    val gjelderFraOgMed: LocalDate?,
    val gjelderTilOgMed: LocalDate?,
    val begrunnelse: String? = null,
) : VedtakLæremidlerDto(TypeVedtak.INNVILGELSE),
    VedtakLæremidlerResponse

data class InnvilgelseLæremidlerRequest(
    val vedtaksperioder: List<VedtaksperiodeDto>,
    val begrunnelse: String? = null,
) : VedtakLæremidlerDto(TypeVedtak.INNVILGELSE),
    VedtakLæremidlerRequest
