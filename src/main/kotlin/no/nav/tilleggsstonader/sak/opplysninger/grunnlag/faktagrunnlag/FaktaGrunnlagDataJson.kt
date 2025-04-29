package no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * [FaktaGrunnlagDataJson] definierer alle suklasser av [FaktaGrunnlagData]
 * Den mapper riktig type [JsonSubTypes.Type.name] til riktig klass den skal deserialisere til
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true,
)
@JsonSubTypes(
    JsonSubTypes.Type(FaktaGrunnlagPersonopplysninger::class, name = "PERSONOPPLYSNINGER"),
    JsonSubTypes.Type(FaktaGrunnlagBarnAndreForeldreSaksinformasjon::class, name = "BARN_ANDRE_FORELDRE_SAKSINFORMASJON"),
    JsonSubTypes.Type(FaktaGrunnlagArenaVedtak::class, name = "ARENA_VEDTAK_TOM"),
    failOnRepeatedNames = true,
)
sealed interface FaktaGrunnlagDataJson
