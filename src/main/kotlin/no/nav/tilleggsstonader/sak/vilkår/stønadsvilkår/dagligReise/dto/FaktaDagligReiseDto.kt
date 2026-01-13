package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaPrivatBil
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
)
sealed interface FaktaDagligReiseDto {
    val type: TypeDagligReise

    fun mapTilFakta(adresse: String?): FaktaDagligReise
}

data class FaktaDagligReiseOffentligTransportDto(
    val reiseId: ReiseId,
    val reisedagerPerUke: Int,
    val prisEnkelbillett: Int?,
    val prisSyvdagersbillett: Int?,
    val prisTrettidagersbillett: Int?,
) : FaktaDagligReiseDto {
    override val type = TypeDagligReise.OFFENTLIG_TRANSPORT

    override fun mapTilFakta(adresse: String?) =
        FaktaOffentligTransport(
            reiseId = reiseId,
            reisedagerPerUke = reisedagerPerUke,
            prisEnkelbillett = prisEnkelbillett,
            prisTrettidagersbillett = prisTrettidagersbillett,
            prisSyvdagersbillett = prisSyvdagersbillett,
            adresse = adresse,
        )
}

data class FaktaDagligReisePrivatBilDto(
    val reiseId: ReiseId,
    val reisedagerPerUke: Int,
    val reiseavstandEnVei: BigDecimal,
    val bompengerEnVei: Int?,
    val fergekostandEnVei: Int?,
) : FaktaDagligReiseDto {
    override val type = TypeDagligReise.PRIVAT_BIL

    override fun mapTilFakta(adresse: String?) =
        FaktaPrivatBil(
            reiseId = reiseId,
            reisedagerPerUke = reisedagerPerUke,
            reiseavstandEnVei = reiseavstandEnVei,
            bompengerEnVei = bompengerEnVei,
            fergekostandEnVei = fergekostandEnVei,
            adresse = adresse,
        )
}
