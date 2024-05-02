package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes(
    JsonSubTypes.Type(InnvilgelseTilsynBarnDto::class, name = "INNVILGELSE"),
    JsonSubTypes.Type(AvslagTilsynBarnDto::class, name = "AVSLAG"),
)
sealed class VedtakTilsynBarnDto(open val type: TypeVedtak)
