package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør

/**
 * Gjenbrukes både for Request og Response foreløpig
 */
data class OpphørTilsynBarnDto(
    val årsakerOpphør: List<ÅrsakOpphør>,
    val begrunnelse: String,
) : VedtakTilsynBarnRequest, VedtakTilsynBarnResponse {
    override val type: TypeVedtak = TypeVedtak.OPPHØR
}

@Deprecated("Bruk OpphørTilsynBarnDto når frontend sender med typeVedtak")
data class OpphørRequest(
    val årsakerOpphør: List<ÅrsakOpphør>,
    val begrunnelse: String,
) {
    fun tilDto() = OpphørTilsynBarnDto(årsakerOpphør, begrunnelse)
}
