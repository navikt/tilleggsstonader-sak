package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.ÅrsakOpphør

data class OpphørTilsynBarnDto(
    val årsakerOpphør: List<ÅrsakOpphør>,
    val begrunnelse: String,
) : VedtakTilsynBarnDto(TypeVedtak.OPPHØR)

data class OpphørRequest(
    val årsakerOpphør: List<ÅrsakOpphør>,
    val begrunnelse: String,
) {
    fun tilDto() = OpphørTilsynBarnDto(årsakerOpphør, begrunnelse)
}
