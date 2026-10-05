package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.libs.feil.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.TypeReiseformål
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
    JsonSubTypes.Type(FaktaReiseTilSamlingOffentligTransport::class, name = "REISE_TIL_SAMLING_OFFENTLIG_TRANSPORT"),
    JsonSubTypes.Type(FaktaReiseTilSamlingPrivatBil::class, name = "REISE_TIL_SAMLING_PRIVAT_BIL"),
    JsonSubTypes.Type(FaktaReiseTilSamlingUbestemt::class, name = "REISE_TIL_SAMLING_UBESTEMT"),
    JsonSubTypes.Type(
        FaktaReiseOppstartAvslutningHjemreiseOffentligTransport::class,
        name = "REISE_OPPSTART_AVSLUTNING_HJEMREISE_OFFENTLIG_TRANSPORT",
    ),
    JsonSubTypes.Type(
        FaktaReiseOppstartAvslutningHjemreisePrivatBil::class,
        name = "REISE_OPPSTART_AVSLUTNING_HJEMREISE_PRIVAT_BIL",
    ),
    JsonSubTypes.Type(
        FaktaReiseOppstartAvslutningHjemreiseUbestemt::class,
        name = "REISE_OPPSTART_AVSLUTNING_HJEMREISE_UBESTEMT",
    ),
    JsonSubTypes.Type(FaktaFlyttebyrå::class, name = "FLYTTING_FLYTTEBYRÅ"),
    JsonSubTypes.Type(FaktaKjøreSelv::class, name = "FLYTTING_KJØRE_SELV"),
    JsonSubTypes.Type(FaktaFlyttingUbestemt::class, name = "FLYTTING_UBESTEMT"),
    failOnRepeatedNames = true,
)
sealed interface VilkårFakta

sealed interface ReiseVilkårFakta : VilkårFakta {
    val reiseId: ReiseId
    val adresse: String?
}

sealed interface FlyttingVilkårFakta : VilkårFakta {
    val adresse: String?
}

data class FaktaFlyttebyrå(
    val tilbud1: FlyttebyråTilbud,
    val tilbud2: FlyttebyråTilbud,
    override val adresse: String? = null,
) : FlyttingVilkårFakta

data class FlyttebyråTilbud(
    val navn: String?,
    val pris: Int?,
)

data class FaktaKjøreSelv(
    val avstandEnVei: Int?,
    val henger: Int?,
    val bompenger: Int?,
    val ferge: Int?,
    val parkering: Int?,
    override val adresse: String? = null,
) : FlyttingVilkårFakta

data class FaktaFlyttingUbestemt(
    override val adresse: String? = null,
) : FlyttingVilkårFakta

data class FaktaReiseTilSamlingOffentligTransport(
    override val reiseId: ReiseId,
    override val adresse: String?,
    val utgifterOffentligTransport: BigDecimal,
    val begrunnelse: String,
    val aktivitetId: VilkårperiodeGlobalId? = null,
) : ReiseVilkårFakta

data class FaktaReiseTilSamlingPrivatBil(
    override val reiseId: ReiseId,
    override val adresse: String?,
    val reiseavstand: BigDecimal,
    val begrunnelse: String?,
    val aktivitetId: VilkårperiodeGlobalId? = null,
    val bompenger: BigDecimal? = null,
    val fergekostnad: BigDecimal? = null,
    val parkering: BigDecimal? = null,
    val piggdekkavgift: BigDecimal? = null,
) : ReiseVilkårFakta

data class FaktaReiseTilSamlingUbestemt(
    override val reiseId: ReiseId,
    override val adresse: String?,
) : ReiseVilkårFakta

data class FaktaReiseOppstartAvslutningHjemreiseOffentligTransport(
    override val reiseId: ReiseId,
    override val adresse: String?,
    val typeReiseformål: TypeReiseformål,
    val utgifterOffentligTransport: BigDecimal,
    val aktivitetId: VilkårperiodeGlobalId,
) : ReiseVilkårFakta

data class FaktaReiseOppstartAvslutningHjemreisePrivatBil(
    override val reiseId: ReiseId,
    override val adresse: String?,
    val typeReiseformål: TypeReiseformål,
    val reiseavstand: BigDecimal,
    val aktivitetId: VilkårperiodeGlobalId,
    val bompenger: BigDecimal? = null,
    val fergekostnad: BigDecimal? = null,
) : ReiseVilkårFakta

data class FaktaReiseOppstartAvslutningHjemreiseUbestemt(
    override val reiseId: ReiseId,
    override val adresse: String?,
    val typeReiseformål: TypeReiseformål,
) : ReiseVilkårFakta

data class FaktaDagligReiseUbestemt(
    override val reiseId: ReiseId,
    override val adresse: String?,
) : ReiseVilkårFakta

data class FaktaDagligReiseOffentligTransport(
    override val reiseId: ReiseId,
    val reisedagerPerUke: Int,
    val prisEnkelbillett: Int?,
    val prisSyvdagersbillett: Int?,
    val prisTrettidagersbillett: Int?,
    override val adresse: String?,
    val tiltaksvariant: TypeAktivitet? = null,
) : ReiseVilkårFakta

data class FaktaDagligReisePrivatBil(
    override val reiseId: ReiseId,
    val reiseavstandEnVei: BigDecimal,
    val faktaDelperioder: List<FaktaDelperiodePrivatBil>,
    override val adresse: String?,
    val aktivitetId: VilkårperiodeGlobalId,
) : ReiseVilkårFakta

data class FaktaDelperiodePrivatBil(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val reisedagerPerUke: Int,
    val bompengerPerDag: BigDecimal?,
    val fergekostnadPerDag: BigDecimal?,
) : Periode<LocalDate> {
    init {
        brukerfeilHvis(reisedagerPerUke <= 0) {
            "Reisedager per uke må være større enn 0"
        }
        brukerfeilHvis(reisedagerPerUke > 7) {
            "Reisedager per uke kan ikke være mer enn 7"
        }
        brukerfeilHvis(bompengerPerDag != null && bompengerPerDag < BigDecimal.ZERO) {
            "Bompengeprisen må være større enn 0"
        }
        brukerfeilHvis(fergekostnadPerDag != null && fergekostnadPerDag < BigDecimal.ZERO) {
            "Fergekostnaden må være større enn 0"
        }
    }
}

enum class TypeVilkårFakta {
    FLYTTING_FLYTTEBYRÅ,
    FLYTTING_KJØRE_SELV,
    FLYTTING_UBESTEMT,
    DAGLIG_REISE_OFFENTLIG_TRANSPORT,
    DAGLIG_REISE_PRIVAT_BIL,
    DAGLIG_REISE_UBESTEMT,
    REISE_TIL_SAMLING_OFFENTLIG_TRANSPORT,
    REISE_TIL_SAMLING_PRIVAT_BIL,
    REISE_TIL_SAMLING_UBESTEMT,
    REISE_OPPSTART_AVSLUTNING_HJEMREISE_OFFENTLIG_TRANSPORT,
    REISE_OPPSTART_AVSLUTNING_HJEMREISE_PRIVAT_BIL,
    REISE_OPPSTART_AVSLUTNING_HJEMREISE_UBESTEMT,
}
