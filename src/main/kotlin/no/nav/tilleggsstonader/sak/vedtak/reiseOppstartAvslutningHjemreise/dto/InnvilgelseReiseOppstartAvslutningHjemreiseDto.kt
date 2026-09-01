package no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.dto.LagretVedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDomene
import java.time.LocalDate

data class InnvilgelseReiseOppstartAvslutningHjemreiseResponse(
    val vedtaksperioder: List<LagretVedtaksperiodeDto>?,
    val beregningsresultat: BeregningsresultatReiseOppstartAvslutningHjemreiseDto,
    val gjelderFraOgMed: LocalDate?,
    val gjelderTilOgMed: LocalDate?,
    val begrunnelse: String? = null,
) : VedtakReiseOppstartAvslutningHjemreiseDto(TypeVedtak.INNVILGELSE),
    VedtakReiseOppstartAvslutningHjemreiseResponse

sealed interface InnvilgelseReiseOppstartAvslutningHjemreiseRequest : VedtakReiseOppstartAvslutningHjemreiseRequest {
    val begrunnelse: String?

    fun vedtaksperioder(): List<Vedtaksperiode>
}

data class InnvilgelseReiseOppstartAvslutningHjemreiseTsoRequest(
    val vedtaksperioder: List<VedtaksperiodeDto>,
    override val begrunnelse: String? = null,
) : InnvilgelseReiseOppstartAvslutningHjemreiseRequest {
    override fun vedtaksperioder(): List<Vedtaksperiode> = vedtaksperioder.tilDomene()
}

data class InnvilgelseReiseOppstartAvslutningHjemreiseTsrRequest(
    val vedtaksperioder: List<VedtaksperiodeDto>,
    override val begrunnelse: String? = null,
) : InnvilgelseReiseOppstartAvslutningHjemreiseRequest {
    override fun vedtaksperioder(): List<Vedtaksperiode> = vedtaksperioder.tilDomene()
}
