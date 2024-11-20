package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag

data class AvslagTilsynBarnDto(
    val årsakerAvslag: List<ÅrsakAvslag>,
    val begrunnelse: String,
) : VedtakTilsynBarnDto(TypeVedtak.AVSLAG)

data class AvslagRequest(
    val årsakerAvslag: List<ÅrsakAvslag>,
    val begrunnelse: String,
) {
    fun tilDto() = AvslagTilsynBarnDto(årsakerAvslag, begrunnelse)
}
