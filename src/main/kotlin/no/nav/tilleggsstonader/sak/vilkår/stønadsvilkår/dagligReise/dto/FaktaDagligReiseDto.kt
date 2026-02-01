package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaUbestemtType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import java.math.BigDecimal

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true,
)
@JsonSubTypes(
    JsonSubTypes.Type(FaktaDagligReiseOffentligTransportDto::class, name = "OFFENTLIG_TRANSPORT"),
    JsonSubTypes.Type(FaktaDagligReisePrivatBilDto::class, name = "PRIVAT_BIL"),
    JsonSubTypes.Type(FaktaDagligReiseUbestemtDto::class, name = "UBESTEMT"),
)
sealed interface FaktaDagligReiseDto {
    val type: TypeDagligReise

    fun mapTilFakta(
        reiseId: ReiseId,
        adresse: String?,
    ): FaktaDagligReise
}

data class FaktaDagligReiseOffentligTransportDto(
    val reisedagerPerUke: Int,
    val prisEnkelbillett: Int?,
    val prisSyvdagersbillett: Int?,
    val prisTrettidagersbillett: Int?,
) : FaktaDagligReiseDto {
    override val type = TypeDagligReise.OFFENTLIG_TRANSPORT

    override fun mapTilFakta(
        reiseId: ReiseId,
        adresse: String?,
    ) = FaktaOffentligTransport(
        reiseId = reiseId,
        adresse = adresse,
        reisedagerPerUke = reisedagerPerUke,
        prisEnkelbillett = prisEnkelbillett,
        prisTrettidagersbillett = prisTrettidagersbillett,
        prisSyvdagersbillett = prisSyvdagersbillett,
    )
}

data class FaktaDagligReisePrivatBilDto(
    val reisedagerPerUke: Int,
    val reiseavstandEnVei: BigDecimal,
    val bompengerEnVei: Int?,
    val fergekostandEnVei: Int?,
) : FaktaDagligReiseDto {
    override val type = TypeDagligReise.PRIVAT_BIL

    override fun mapTilFakta(
        reiseId: ReiseId,
        adresse: String?,
    ) = FaktaPrivatBil(
        reiseId = reiseId,
        adresse = adresse,
        reisedagerPerUke = reisedagerPerUke,
        reiseavstandEnVei = reiseavstandEnVei,
        bompengerEnVei = bompengerEnVei,
        fergekostandEnVei = fergekostandEnVei,
    )
}

data object FaktaDagligReiseUbestemtDto : FaktaDagligReiseDto {
    override val type = TypeDagligReise.UBESTEMT

    override fun mapTilFakta(
        reiseId: ReiseId,
        adresse: String?,
    ) = FaktaUbestemtType(
        reiseId = reiseId,
        adresse = adresse,
    )
}
