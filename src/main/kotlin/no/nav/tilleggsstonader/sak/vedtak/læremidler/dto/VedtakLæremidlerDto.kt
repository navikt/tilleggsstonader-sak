package no.nav.tilleggsstonader.sak.vedtak.læremidler.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakRequest
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse

sealed class VedtakLæremidlerDto(
    open val type: TypeVedtak,
)

sealed interface VedtakLæremidlerRequest : VedtakRequest

sealed interface VedtakLæremidlerResponse : VedtakResponse
