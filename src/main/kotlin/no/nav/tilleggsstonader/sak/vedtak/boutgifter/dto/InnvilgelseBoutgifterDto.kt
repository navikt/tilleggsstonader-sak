package no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import java.time.LocalDate

/**
 * @param vedtaksperioder viser alle vedtaksperioder, inkl vedtaksperioder før revurder-fra dato
 * @param beregningsresultat viser beregningsresultatet fra og med revurder-fra-dato
 * Beregningsresultatet kan være en del av en løpende måned. Eks 1jan-31jan med revurder-fra 15 gir 15jan-31jan
 * @param gjelderFraOgMed gjelder fra og med revurder-fra-dato, brukes til brevet for å vise fra når vedtaket gjelder fra
 */
data class InnvilgelseBoutgifterResponse(
    val vedtaksperioder: List<VedtaksperiodeDto>,
    val beregningsresultat: BeregningsresultatBoutgifterDto,
    val gjelderFraOgMed: LocalDate,
    val gjelderTilOgMed: LocalDate,
    val begrunnelse: String? = null,
) : VedtakBoutgifterDto(TypeVedtak.INNVILGELSE),
    VedtakBoutgifterResponse

data class InnvilgelseBoutgifterRequest(
    val vedtaksperioder: List<VedtaksperiodeDto>,
    val begrunnelse: String? = null,
) : VedtakBoutgifterRequest
