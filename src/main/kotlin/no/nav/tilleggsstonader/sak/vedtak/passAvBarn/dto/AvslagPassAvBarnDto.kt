package no.nav.tilleggsstonader.sak.vedtak.passAvBarn.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag

data class AvslagPassAvBarnDto(
    val årsakerAvslag: List<ÅrsakAvslag>,
    val begrunnelse: String,
) : VedtakPassAvBarnDto(TypeVedtak.AVSLAG),
    VedtakPassAvBarnRequest,
    VedtakPassAvBarnResponse
