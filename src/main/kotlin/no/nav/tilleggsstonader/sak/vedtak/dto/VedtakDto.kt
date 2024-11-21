package no.nav.tilleggsstonader.sak.vedtak.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes(
    JsonSubTypes.Type(InnvilgelseTilsynBarnRequest::class, name = "INNVILGELSE_TILSYN_BARN"),
    JsonSubTypes.Type(AvslagTilsynBarnDto::class, name = "AVSLAG_TILSYN_BARN"),
    JsonSubTypes.Type(OpphørDto::class, name = "OPPHØR_TILSYN_BARN"),
)
sealed interface VedtakDto {
    val vedtakType: VedtakType
}

sealed interface VedtakType