package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import java.math.BigDecimal

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
)
@JsonSubTypes(
    JsonSubTypes.Type(FaktaDagligReiseOffentligTransport::class, name = "DAGLIG_REISE_OFFENTLIG_TRANSPORT"),
    JsonSubTypes.Type(FaktaDagligReisePrivatBil::class, name = "DAGLIG_REISE_PRIVAT_BIL"),
    JsonSubTypes.Type(FaktaDagligReiseUbestemt::class, name = "DAGLIG_REISE_UBESTEMT"),
    failOnRepeatedNames = true,
)
sealed interface VilkårFakta {
    val reiseId: ReiseId
    val adresse: String?
}

data class FaktaDagligReiseUbestemt(
    override val reiseId: ReiseId,
    override val adresse: String?,
) : VilkårFakta

data class FaktaDagligReiseOffentligTransport(
    override val reiseId: ReiseId,
    val reisedagerPerUke: Int,
    val prisEnkelbillett: Int?,
    val prisSyvdagersbillett: Int?,
    val prisTrettidagersbillett: Int?,
    override val adresse: String?,
) : VilkårFakta

data class FaktaDagligReisePrivatBil(
    override val reiseId: ReiseId,
    val reisedagerPerUke: Int,
    val reiseavstandEnVei: BigDecimal,
    val bompengerEnVei: Int?,
    val fergekostandEnVei: Int?,
    override val adresse: String?,
) : VilkårFakta

enum class TypeVilkårFakta {
    DAGLIG_REISE_OFFENTLIG_TRANSPORT,
    DAGLIG_REISE_PRIVAT_BIL,
    DAGLIG_REISE_UBESTEMT,
}
