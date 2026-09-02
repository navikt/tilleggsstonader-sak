package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag

data class AvslagReiseTilSamlingDto(
    val årsakerAvslag: List<ÅrsakAvslag>,
    val begrunnelse: String,
) : VedtakReiseTilSamlingDto(TypeVedtak.AVSLAG),
    VedtakReiseTilSamlingRequest,
    VedtakReiseTilSamlingResponse
