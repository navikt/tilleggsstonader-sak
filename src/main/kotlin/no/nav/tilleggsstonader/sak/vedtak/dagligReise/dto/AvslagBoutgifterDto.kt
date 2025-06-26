package no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag

data class AvslagBoutgifterDto(
    val årsakerAvslag: List<ÅrsakAvslag>,
    val begrunnelse: String,
) : VedtakBoutgifterDto(TypeVedtak.AVSLAG),
    VedtakBoutgifterRequest,
    VedtakBoutgifterResponse
