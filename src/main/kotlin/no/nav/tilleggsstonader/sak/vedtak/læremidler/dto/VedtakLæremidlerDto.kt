package no.nav.tilleggsstonader.sak.vedtak.læremidler.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakRequest
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true,
)
@JsonSubTypes(
    JsonSubTypes.Type(AvslagLæremidlerDto::class, name = "AVSLAG"),
    failOnRepeatedNames = true,
)
sealed class VedtakLæremidlerDto(open val type: TypeVedtak)

sealed interface VedtakLæremidlerRequest : VedtakRequest
sealed interface VedtakLæremidlerResponse : VedtakResponse
