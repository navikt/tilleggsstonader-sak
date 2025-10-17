package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.FaktaGrunnlagData
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

typealias VilkårFakta = GeneriskVilkårFakta<out VilkårFaktaData>

@Table("vilkar_fakta")
data class GeneriskVilkårFakta<T : VilkårFaktaData>(
    @Id
    val id: UUID = UUID.randomUUID(),
    val type: TypeVilkårFakta,
    val data: T,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(), // TODO: Må denne alltid med?
)

sealed interface VilkårFaktaData : VilkårFaktaDataJson

/**
 * [FaktaGrunnlagDataJson] definierer alle suklasser av [FaktaGrunnlagData]
 * Den mapper riktig type [JsonSubTypes.Type.name] til riktig klass den skal deserialisere til
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
)
@JsonSubTypes(
    JsonSubTypes.Type(FaktaDagligReiseOffentligTransport::class, name = "DALIG_REISE_OFFENTLIG_TRANSPORT"),
    JsonSubTypes.Type(FaktaDagligReisePrivatBil::class, name = "DALIG_REISE_PRIVAT_BIL"),
    failOnRepeatedNames = true,
)
sealed interface VilkårFaktaDataJson

data class FaktaDagligReiseOffentligTransport(
    val reisedagerPerUke: Int,
    val prisEnkelbillett: Int?,
    val prisSyvdagersbillett: Int?,
    val prisTrettidagersbillett: Int?,
) : VilkårFaktaData

data class FaktaDagligReisePrivatBil(
    val reisedagerPerUke: Int,
    val reiseavstandEnVei: Int,
    val prisBompengerPerDag: Int?,
    val prisFergekostandPerDag: Int?,
) : VilkårFaktaData

enum class TypeVilkårFakta {
    DAGLIG_REISE_OFFENTLIG_TRANSPORT,
    DAGLIG_REISE_PRIVAT_BIL,
}
