package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto

data class InnvilgelseTilsynBarnResponse(
    val beregningsresultat: BeregningsresultatTilsynBarnDto,
    val vedtaksperioder: List<VedtaksperiodeDto>,
) : VedtakTilsynBarnDto(TypeVedtak.INNVILGELSE),
    VedtakTilsynBarnResponse

data object InnvilgelseTilsynBarnRequest :
    VedtakTilsynBarnRequest, VedtakTilsynBarnDto(TypeVedtak.INNVILGELSE)

data class InnvilgelseTilsynBarnRequestV2(
    val vedtaksperioder: List<VedtaksperiodeDto>,
) : VedtakTilsynBarnRequest
