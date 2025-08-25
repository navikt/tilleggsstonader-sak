package no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakRequest
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse

sealed class VedtakDagligReiseDto(
    open val type: TypeVedtak,
)

sealed interface VedtakDagligReiseRequest : VedtakRequest

sealed interface VedtakDagligReiseResponse : VedtakResponse
