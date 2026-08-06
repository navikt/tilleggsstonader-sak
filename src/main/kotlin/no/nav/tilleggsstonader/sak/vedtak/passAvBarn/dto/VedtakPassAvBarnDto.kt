package no.nav.tilleggsstonader.sak.vedtak.passAvBarn.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakRequest
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse

sealed class VedtakPassAvBarnDto(
    open val type: TypeVedtak,
)

sealed interface VedtakPassAvBarnRequest : VedtakRequest

sealed interface VedtakPassAvBarnResponse : VedtakResponse
