package no.nav.tilleggsstonader.sak.vedtak.domain

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer

/**
 * [VedtaksdataJson] definierer alle subklasser av [Vedtaksdata]
 * Den mapper riktig type [JsonSubTypes.Type.name] til riktig klasse den skal deserialisere til
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true,
)
@JsonSubTypes(
    JsonSubTypes.Type(InnvilgelsePassAvBarn::class, name = "INNVILGELSE_TILSYN_BARN"),
    JsonSubTypes.Type(OpphørPassAvBarn::class, name = "OPPHØR_TILSYN_BARN"),
    JsonSubTypes.Type(AvslagPassAvBarn::class, name = "AVSLAG_TILSYN_BARN"),
    JsonSubTypes.Type(InnvilgelseLæremidler::class, name = "INNVILGELSE_LÆREMIDLER"),
    JsonSubTypes.Type(AvslagLæremidler::class, name = "AVSLAG_LÆREMIDLER"),
    JsonSubTypes.Type(OpphørLæremidler::class, name = "OPPHØR_LÆREMIDLER"),
    JsonSubTypes.Type(InnvilgelseBoutgifter::class, name = "INNVILGELSE_BOUTGIFTER"),
    JsonSubTypes.Type(AvslagBoutgifter::class, name = "AVSLAG_BOUTGIFTER"),
    JsonSubTypes.Type(OpphørBoutgifter::class, name = "OPPHØR_BOUTGIFTER"),
    JsonSubTypes.Type(InnvilgelseDagligReise::class, name = "INNVILGELSE_DAGLIG_REISE"),
    JsonSubTypes.Type(AvslagDagligReise::class, name = "AVSLAG_DAGLIG_REISE"),
    JsonSubTypes.Type(OpphørDagligReise::class, name = "OPPHØR_DAGLIG_REISE"),
    JsonSubTypes.Type(InnvilgelseReiseTilSamling::class, name = "INNVILGELSE_REISE_TIL_SAMLING"),
    failOnRepeatedNames = true,
)
sealed interface VedtaksdataJson

/**
 * Enums for de ulike klassene implementerer [TypeVedtaksdata] som er en sealed interface
 * For å kunne deserialisere disse trenger jackson litt hjelp.
 * Den finner riktig enum ut fra hvilken
 */
class TypeVedtaksdataDeserializer : ValueDeserializer<TypeVedtaksdata>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): TypeVedtaksdata = typerVedtaksdata[p.string] ?: error("Finner ikke mapping for ${p.string}")
}

val typerVedtaksdata: Map<String, TypeVedtaksdata> =
    listOf(
        TypeVedtakPassAvBarn.entries,
        TypeVedtakLæremidler.entries,
        TypeVedtakBoutgifter.entries,
        TypeVedtakDagligReise.entries,
        TypeVedtakReiseTilSamling.entries,
    ).flatten().associateBy { it.name }
