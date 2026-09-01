package no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.dto

import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakRequest
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse

sealed class VedtakReiseOppstartAvslutningHjemreiseDto(
    open val type: TypeVedtak,
)

sealed interface VedtakReiseOppstartAvslutningHjemreiseRequest : VedtakRequest

sealed interface VedtakReiseOppstartAvslutningHjemreiseResponse : VedtakResponse
