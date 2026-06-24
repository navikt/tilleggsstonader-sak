package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto

import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDomene

//
// data class InnvilgelseReiseTilSamlingResponse(
//    val vedtaksperioder: List<LagretVedtaksperiodeDto>?,
//    val beregningsresultat: BeregningsResultatReiseTilSamlingDto,
//    val gjelderFraOgMed: LocalDate?,
//    val gjelderTilOgMed: LocalDate?,
//    val begrunnelse: String? = null,
// ) : VedtakReiseTilSamlingDto(TypeVedtak.INNVILGELSE),
//    VedtakReiseTilSamlingResponse
sealed interface InnvilgelseReiseTilSamlingRequest : VedtakReiseTilSamlingRequest {
    val begrunnelse: String?

    fun vedtaksperioder(): List<Vedtaksperiode>
}

data class InnvilgelseReiseTilSamlingTsoRequest(
    val vedtaksperioder: List<VedtaksperiodeDto>,
    override val begrunnelse: String? = null,
) : InnvilgelseReiseTilSamlingRequest {
    override fun vedtaksperioder(): List<Vedtaksperiode> = vedtaksperioder.tilDomene()
}
