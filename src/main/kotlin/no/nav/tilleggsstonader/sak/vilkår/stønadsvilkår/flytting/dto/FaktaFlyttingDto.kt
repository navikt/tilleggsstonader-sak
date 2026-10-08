package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FlyttingId

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true,
)
@JsonSubTypes(
    JsonSubTypes.Type(FaktaFlyttebyråDto::class, name = "FLYTTING_FLYTTEBYRÅ"),
    JsonSubTypes.Type(FaktaFlytteSelvDto::class, name = "FLYTTING_FLYTTE_SELV"),
    JsonSubTypes.Type(FaktaFlyttingUbestemtDto::class, name = "FLYTTING_UBESTEMT"),
)
sealed interface FaktaFlyttingDto {
    val flyttingId: FlyttingId
    val type: TypeFaktaFlytting
    val adresse: String
}

enum class TypeFaktaFlytting {
    FLYTTING_FLYTTEBYRÅ,
    FLYTTING_FLYTTE_SELV,
    FLYTTING_UBESTEMT,
}

data class FaktaFlyttebyråDto(
    val tilbud1: FlyttebyråTilbudDto = FlyttebyråTilbudDto(),
    val tilbud2: FlyttebyråTilbudDto = FlyttebyråTilbudDto(),
    val erBetalingDokumentert: Boolean,
    override val adresse: String,
    override val flyttingId: FlyttingId = FlyttingId.random(),
) : FaktaFlyttingDto {
    override val type = TypeFaktaFlytting.FLYTTING_FLYTTEBYRÅ
}

data class FlyttebyråTilbudDto(
    val navn: String? = null,
    val pris: Int? = null,
)

data class FaktaFlytteSelvDto(
    val avstandEnVei: Int? = null,
    val henger: Int? = null,
    val bompenger: Int? = null,
    val ferge: Int? = null,
    val parkering: Int? = null,
    override val adresse: String,
    override val flyttingId: FlyttingId = FlyttingId.random(),
) : FaktaFlyttingDto {
    override val type = TypeFaktaFlytting.FLYTTING_FLYTTE_SELV
}

data class FaktaFlyttingUbestemtDto(
    override val adresse: String,
    override val flyttingId: FlyttingId = FlyttingId.random(),
) : FaktaFlyttingDto {
    override val type = TypeFaktaFlytting.FLYTTING_UBESTEMT
}
