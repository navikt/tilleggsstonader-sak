package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakRequest
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse

sealed class VedtakReiseTilSamlingDto(
    open val type: TypeVedtak,
)

sealed interface VedtakReiseTilSamlingRequest : VedtakRequest

sealed interface VedtakReiseTilSamlingResponse : VedtakResponse
