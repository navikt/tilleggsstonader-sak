package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak

data class InnvilgelseTilsynBarnResponse(
    val beregningsresultat: BeregningsresultatTilsynBarnDto,
): VedtakTilsynBarnResponse {
    override val type: TypeVedtak = TypeVedtak.INNVILGELSE
}

data object InnvilgelseTilsynBarnRequest : VedtakTilsynBarnRequest {
    override val type: TypeVedtak = TypeVedtak.INNVILGELSE
}

/**
 * TODO
 */
@Deprecated("Bruk InnvilgelseTilsynBarnRequest når frontend sender med typeVedtak")
data object InnvilgelseTilsynBarnRequestGammel {
    fun tilDto() = InnvilgelseTilsynBarnRequest
}
