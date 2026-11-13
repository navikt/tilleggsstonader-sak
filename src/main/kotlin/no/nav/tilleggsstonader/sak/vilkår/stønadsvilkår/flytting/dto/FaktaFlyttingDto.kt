package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.NødvendigUtgiftDto
import java.math.BigDecimal

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
    val type: TypeFaktaFlytting
    val adresse: String
}

enum class TypeFaktaFlytting {
    FLYTTING_FLYTTEBYRÅ,
    FLYTTING_FLYTTE_SELV,
    FLYTTING_UBESTEMT,
}

data class FaktaFlyttebyråDto(
    val tilbud: List<FlyttebyråTilbudDto>,
    val dokumenterteUtgifter: DokumenterteUtgifterDto,
    val nødvendigUtgift: NødvendigUtgiftDto,
    override val adresse: String,
    val beløpSomKanDekkes: BigDecimal?,
) : FaktaFlyttingDto {
    override val type = TypeFaktaFlytting.FLYTTING_FLYTTEBYRÅ
}

data class FlyttebyråTilbudDto(
    val navn: String? = null,
    val pris: Int? = null,
)

data class FaktaFlytteSelvDto(
    val avstandEnVei: Int? = null,
    val henger: BigDecimal? = null,
    val leiebil: BigDecimal? = null,
    val bompenger: BigDecimal? = null,
    val ferge: BigDecimal? = null,
    val parkering: BigDecimal? = null,
    val annet: BigDecimal? = null,
    val nødvendigUtgift: NødvendigUtgiftDto,
    override val adresse: String,
    val dokumenterteUtgifter: DokumenterteUtgifterDto,
) : FaktaFlyttingDto {
    override val type = TypeFaktaFlytting.FLYTTING_FLYTTE_SELV
}

data class FaktaFlyttingUbestemtDto(
    override val adresse: String,
) : FaktaFlyttingDto {
    override val type = TypeFaktaFlytting.FLYTTING_UBESTEMT
}
