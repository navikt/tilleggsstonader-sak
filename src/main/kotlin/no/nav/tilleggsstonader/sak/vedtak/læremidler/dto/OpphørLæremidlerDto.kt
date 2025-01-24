package no.nav.tilleggsstonader.sak.vedtak.læremidler.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør

data class OpphørLæremidlerResponse(
    val vedtaksperioder: List<VedtaksperiodeDto>,
    val årsakerOpphør: List<ÅrsakOpphør>,
    val begrunnelse: String,
) : VedtakLæremidlerResponse, VedtakLæremidlerDto(TypeVedtak.OPPHØR)

data class OpphørLæremidlerRequest(
    val årsakerOpphør: List<ÅrsakOpphør>,
    val begrunnelse: String,
) : VedtakLæremidlerRequest, VedtakLæremidlerDto(TypeVedtak.OPPHØR)
