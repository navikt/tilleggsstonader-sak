package no.nav.tilleggsstonader.sak.vedtak.passAvBarn.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dto.LagretVedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto

data class InnvilgelsePassAvBarnResponse(
    val beregningsresultat: BeregningsresultatPassAvBarnDto,
    val vedtaksperioder: List<LagretVedtaksperiodeDto>?,
    val begrunnelse: String? = null,
) : VedtakPassAvBarnDto(TypeVedtak.INNVILGELSE),
    VedtakPassAvBarnResponse

data class InnvilgelsePassAvBarnRequest(
    val vedtaksperioder: List<VedtaksperiodeDto>,
    val begrunnelse: String? = null,
) : VedtakPassAvBarnRequest
