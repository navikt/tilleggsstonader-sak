package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaPrivatBil

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes(
    JsonSubTypes.Type(FaktaDagligReiseOffentligTransportDto::class, name = "FAKTA_DAGLIG_REISE_OFFENTLIG_TRANSPORT"),
    JsonSubTypes.Type(FaktaDagligReisePrivatBilDto::class, name = "FAKTA_DAGLIG_REISE_PRIVAT_BIL"),
)
sealed interface FaktaDagligReiseDto {
    val type: TypeDagligReise

    fun mapTilFakta(): FaktaDagligReise
}

data class FaktaDagligReiseOffentligTransportDto(
    val reisedagerPerUke: Int,
    val prisEnkelbillett: Int?,
    val prisSyvdagersbillett: Int?,
    val prisTrettidagersbillett: Int?,
) : FaktaDagligReiseDto {
    override val type = TypeDagligReise.OFFENTLIG_TRANSPORT

    override fun mapTilFakta() =
        FaktaOffentligTransport(
            reisedagerPerUke = reisedagerPerUke,
            prisEnkelbillett = prisEnkelbillett,
            prisTrettidagersbillett = prisTrettidagersbillett,
            prisSyvdagersbillett = prisSyvdagersbillett,
        )
}

data class FaktaDagligReisePrivatBilDto(
    val reisedagerPerUke: Int,
    val reiseavstandEnVei: Int,
    val prisBompengerPerDag: Int?,
    val prisFergekostandPerDag: Int?,
) : FaktaDagligReiseDto {
    override val type = TypeDagligReise.PRIVAT_BIL

    override fun mapTilFakta() =
        FaktaPrivatBil(
            reisedagerPerUke = reisedagerPerUke,
            reiseavstandEnVei = reiseavstandEnVei,
            prisBompengerPerDag = prisBompengerPerDag,
            prisFergekostandPerDag = prisFergekostandPerDag,
        )
}
