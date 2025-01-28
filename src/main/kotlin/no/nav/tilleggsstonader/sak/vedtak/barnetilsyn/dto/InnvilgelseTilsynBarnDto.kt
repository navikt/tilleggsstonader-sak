package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak

data class InnvilgelseTilsynBarnResponse(
    val beregningsresultat: BeregningsresultatTilsynBarnDto,
) : VedtakTilsynBarnDto(TypeVedtak.INNVILGELSE),
    VedtakTilsynBarnResponse

data object InnvilgelseTilsynBarnRequest :
    VedtakTilsynBarnRequest, VedtakTilsynBarnDto(TypeVedtak.INNVILGELSE)
