package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak

data class InnvilgelseTilsynBarnResponse(
    val beregningsresultat: BeregningsresultatTilsynBarnDto,
) : VedtakTilsynBarnResponse, VedtakTilsynBarnDto(TypeVedtak.INNVILGELSE)

data object InnvilgelseTilsynBarnRequest :
    VedtakTilsynBarnRequest, VedtakTilsynBarnDto(TypeVedtak.INNVILGELSE)
