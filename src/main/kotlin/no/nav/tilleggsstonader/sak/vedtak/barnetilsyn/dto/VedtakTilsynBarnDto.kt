package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakRequest
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse

sealed class VedtakTilsynBarnDto(
    open val type: TypeVedtak,
)

sealed interface VedtakTilsynBarnRequest : VedtakRequest

sealed interface VedtakTilsynBarnResponse : VedtakResponse
