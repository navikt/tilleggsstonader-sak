package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.FaktaGrunnlagData
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId

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
    JsonSubTypes.Type(FaktaDagligReiseOffentligTransport::class, name = "DAGLIG_REISE_OFFENTLIG_TRANSPORT"),
    JsonSubTypes.Type(FaktaDagligReisePrivatBil::class, name = "DAGLIG_REISE_PRIVAT_BIL"),
    failOnRepeatedNames = true,
)
sealed interface VilkårFakta {
    val type: TypeVilkårFakta
}

data class FaktaDagligReiseOffentligTransport(
    val reiseId: ReiseId,
    val reisedagerPerUke: Int,
    val prisEnkelbillett: Int?,
    val prisSyvdagersbillett: Int?,
    val prisTrettidagersbillett: Int?,
) : VilkårFakta {
    override val type = TypeVilkårFakta.DAGLIG_REISE_OFFENTLIG_TRANSPORT
}

data class FaktaDagligReisePrivatBil(
    val reisedagerPerUke: Int,
    val reiseavstandEnVei: Int,
    val prisBompengerPerDag: Int?,
    val prisFergekostandPerDag: Int?,
) : VilkårFakta {
    override val type = TypeVilkårFakta.DAGLIG_REISE_PRIVAT_BIL
}

enum class TypeVilkårFakta {
    DAGLIG_REISE_OFFENTLIG_TRANSPORT,
    DAGLIG_REISE_PRIVAT_BIL,
}
