package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto

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
    JsonSubTypes.Type(InnvilgelseTilsynBarnRequest::class, name = "INNVILGELSE"),
    JsonSubTypes.Type(AvslagTilsynBarnDto::class, name = "AVSLAG"),
    JsonSubTypes.Type(OpphørTilsynBarnRequest::class, name = "OPPHØR"),
    failOnRepeatedNames = true,
)
sealed class VedtakTilsynBarnDto(open val type: TypeVedtak)

sealed interface VedtakTilsynBarnRequest : VedtakRequest
sealed interface VedtakTilsynBarnResponse : VedtakResponse
