package no.nav.tilleggsstonader.sak.vedtak.læremidler.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør

data class OpphørLæremidlerResponse(
    val vedtaksperioder: List<VedtaksperiodeDto>,
    val årsakerOpphør: List<ÅrsakOpphør>,
    val begrunnelse: String,
) : VedtakLæremidlerDto(TypeVedtak.OPPHØR),
    VedtakLæremidlerResponse

data class OpphørLæremidlerRequest(
    val årsakerOpphør: List<ÅrsakOpphør>,
    val begrunnelse: String,
) : VedtakLæremidlerDto(TypeVedtak.OPPHØR),
    VedtakLæremidlerRequest
