package no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakRequest
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse

sealed class VedtakBoutgifterDto(
    open val type: TypeVedtak,
)

sealed interface VedtakBoutgifterRequest : VedtakRequest

sealed interface VedtakBoutgifterResponse : VedtakResponse
