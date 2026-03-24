package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.FaktaUbestemtType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaDelperiodePrivatBil
import java.math.BigDecimal
import java.time.LocalDate

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
    val reiseavstandEnVei: BigDecimal,
    val faktaDelperioder: List<FaktaDelperiodePrivatBilDto>,
    val adresse: String?,
) : FaktaDagligReiseDto {
    override val type = TypeDagligReise.PRIVAT_BIL

    override fun mapTilFakta(
        reiseId: ReiseId,
        adresse: String?,
    ) = FaktaPrivatBil(
        reiseId = reiseId,
        reiseavstandEnVei = reiseavstandEnVei,
        faktaDelperioder =
            faktaDelperioder.map {
                FaktaDelperiodePrivatBil(
                    fom = it.fom,
                    tom = it.tom,
                    reisedagerPerUke = it.reisedagerPerUke,
                    bompengerEnVei = it.bompengerEnVei,
                    fergekostandEnVei = it.fergekostandEnVei,
                )
            },
        adresse = adresse,
    )
}

data class FaktaDelperiodePrivatBilDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val reisedagerPerUke: Int,
    val bompengerEnVei: Int?,
    val fergekostandEnVei: Int?,
)

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
