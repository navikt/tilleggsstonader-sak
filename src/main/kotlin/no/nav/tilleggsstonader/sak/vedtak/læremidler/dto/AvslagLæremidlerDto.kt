package no.nav.tilleggsstonader.sak.vedtak.læremidler.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag

data class AvslagLæremidlerDto(
    val årsakerAvslag: List<ÅrsakAvslag>,
    val begrunnelse: String,
) : VedtakLæremidlerDto(TypeVedtak.AVSLAG),
    VedtakLæremidlerRequest,
    VedtakLæremidlerResponse
