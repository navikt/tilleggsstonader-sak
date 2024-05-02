package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak

data class AvslagTilsynBarnDto(
    val begrunnelse: String,
) : VedtakTilsynBarnDto(TypeVedtak.AVSLÅTT)

data class AvslagRequest(
    val begrunnelse: String,
) {
    fun tilDto() = AvslagTilsynBarnDto(begrunnelse)
}
