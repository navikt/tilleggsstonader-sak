package no.nav.tilleggsstonader.sak.vedtak.domain

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer

/**
 * [VedtaksdataJson] definierer alle suklasser av [Vedtaksdata]
 * Den mapper riktig type [JsonSubTypes.Type.name] til riktig klass den skal deserialisere til
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true,
)
@JsonSubTypes(
    JsonSubTypes.Type(InnvilgelseTilsynBarn::class, name = "INNVILGELSE_TILSYN_BARN"),
    JsonSubTypes.Type(OpphørTilsynBarn::class, name = "OPPHØR_TILSYN_BARN"),
    JsonSubTypes.Type(AvslagTilsynBarn::class, name = "AVSLAG_TILSYN_BARN"),
    failOnRepeatedNames = true,
)
sealed interface VedtaksdataJson

/**
 * Enums for de ulike klassene implementerer [TypeVedtaksdata] som er en sealed interface
 * For å kunne deserialisere disse trenger jackson litt hjelp.
 * Den finner riktig enum ut fra hvilken
 */
class TypeVedtaksdataDeserializer : JsonDeserializer<TypeVedtaksdata>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): TypeVedtaksdata {
        return typerVedtaksdata[p.text] ?: error("Finner ikke mapping for ${p.text}")
    }
}
val typerVedtaksdata: Map<String, TypeVedtaksdata> =
    listOf(
        TypeVedtakTilsynBarn.entries,
    ).flatten().associateBy { it.name }
