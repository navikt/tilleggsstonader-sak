package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import java.math.BigDecimal
import java.time.LocalDate

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
    val reiseavstandEnVei: BigDecimal,
    val faktaDelperioder: List<FaktaDelperiodePrivatBil>,
    override val adresse: String?,
    val aktivitetId: VilkårperiodeGlobalId,
) : VilkårFakta

data class FaktaDelperiodePrivatBil(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val reisedagerPerUke: Int,
    val bompengerPerDag: Int?,
    val fergekostnadPerDag: Int?,
) : Periode<LocalDate> {
    init {
        validatePeriode()
        brukerfeilHvis(reisedagerPerUke <= 0) {
            "Reisedager per uke må være større enn 0"
        }
        brukerfeilHvis(reisedagerPerUke > 7) {
            "Reisedager per uke kan ikke være mer enn 7"
        }
        brukerfeilHvis(bompengerPerDag != null && bompengerPerDag < 0) {
            "Bompengeprisen må være større enn 0"
        }
        brukerfeilHvis(fergekostnadPerDag != null && fergekostnadPerDag < 0) {
            "Fergekostnaden må være større enn 0"
        }
    }
}

enum class TypeVilkårFakta {
    DAGLIG_REISE_OFFENTLIG_TRANSPORT,
    DAGLIG_REISE_PRIVAT_BIL,
    DAGLIG_REISE_UBESTEMT,
}
