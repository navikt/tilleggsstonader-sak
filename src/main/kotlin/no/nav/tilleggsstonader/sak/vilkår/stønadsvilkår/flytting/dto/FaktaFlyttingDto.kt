package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true,
)
@JsonSubTypes(
    JsonSubTypes.Type(FaktaFlyttebyråDto::class, name = "FLYTTING_FLYTTEBYRÅ"),
    JsonSubTypes.Type(FaktaKjøreSelvDto::class, name = "FLYTTING_KJØRE_SELV"),
    JsonSubTypes.Type(FaktaFlyttingUbestemtDto::class, name = "FLYTTING_UBESTEMT"),
)
sealed interface FaktaFlyttingDto {
    val type: TypeFaktaFlytting
    val adresse: String?
}

enum class TypeFaktaFlytting {
    FLYTTING_FLYTTEBYRÅ,
    FLYTTING_KJØRE_SELV,
    FLYTTING_UBESTEMT,
}

data class FaktaFlyttebyråDto(
    val tilbud1: FlyttebyråTilbudDto = FlyttebyråTilbudDto(),
    val tilbud2: FlyttebyråTilbudDto = FlyttebyråTilbudDto(),
    override val adresse: String? = null,
) : FaktaFlyttingDto {
    override val type = TypeFaktaFlytting.FLYTTING_FLYTTEBYRÅ
}

data class FlyttebyråTilbudDto(
    val navn: String? = null,
    val pris: Int? = null,
)

data class FaktaKjøreSelvDto(
    val avstandEnVei: Int? = null,
    val henger: Int? = null,
    val bompenger: Int? = null,
    val ferge: Int? = null,
    val parkering: Int? = null,
    override val adresse: String? = null,
) : FaktaFlyttingDto {
    override val type = TypeFaktaFlytting.FLYTTING_KJØRE_SELV
}

data class FaktaFlyttingUbestemtDto(
    override val adresse: String? = null,
) : FaktaFlyttingDto {
    override val type = TypeFaktaFlytting.FLYTTING_UBESTEMT
}
