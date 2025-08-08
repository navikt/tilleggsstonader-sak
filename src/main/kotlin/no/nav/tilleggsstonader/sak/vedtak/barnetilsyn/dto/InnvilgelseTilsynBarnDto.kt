package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dto.LagretVedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto

data class InnvilgelseTilsynBarnResponse(
    val beregningsresultat: BeregningsresultatTilsynBarnDto,
    val vedtaksperioder: List<LagretVedtaksperiodeDto>?,
    val begrunnelse: String? = null,
) : VedtakTilsynBarnDto(TypeVedtak.INNVILGELSE),
    VedtakTilsynBarnResponse

data class InnvilgelseTilsynBarnRequest(
    val vedtaksperioder: List<VedtaksperiodeDto>,
    val begrunnelse: String?,
) : VedtakTilsynBarnRequest
