package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag

/**
 * Gjenbrukes både for Request og Response foreløpig
 */
data class AvslagTilsynBarnDto(
    val årsakerAvslag: List<ÅrsakAvslag>,
    val begrunnelse: String,
) : VedtakTilsynBarnRequest, VedtakTilsynBarnResponse {
    override val type: TypeVedtak = TypeVedtak.AVSLAG
}

@Deprecated("Bruk AvslagTilsynBarnDto når frontend sender med typeVedtak")
data class AvslagRequest(
    val årsakerAvslag: List<ÅrsakAvslag>,
    val begrunnelse: String,
) {
    fun tilDto() = AvslagTilsynBarnDto(årsakerAvslag, begrunnelse)
}
