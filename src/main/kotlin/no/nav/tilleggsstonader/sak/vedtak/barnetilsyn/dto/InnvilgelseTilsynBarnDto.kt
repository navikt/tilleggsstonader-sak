package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak

data class InnvilgelseTilsynBarnDto(
    val beregningsresultat: BeregningsresultatTilsynBarnDto?,
) : VedtakTilsynBarnDto(TypeVedtak.INNVILGELSE)

data class InnvilgelseTilsynBarnRequest(
    val beregningsresultat: BeregningsresultatTilsynBarnDto?,
) {
    fun tilDto() = InnvilgelseTilsynBarnDto(beregningsresultat)
}
