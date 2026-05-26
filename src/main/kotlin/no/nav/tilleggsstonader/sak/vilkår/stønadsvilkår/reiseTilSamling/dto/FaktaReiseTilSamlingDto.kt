package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeReiseTilSamling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaReiseTilSamling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.FaktaUbestemtType
import java.math.BigDecimal

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true,
)
@JsonSubTypes(
    JsonSubTypes.Type(FaktaReiseTilSamlingOffentligTransportDto::class, name = "OFFENTLIG_TRANSPORT"),
    JsonSubTypes.Type(FaktaReiseTilSamlingPrivatBilDto::class, name = "PRIVAT_BIL"),
    JsonSubTypes.Type(FaktaReiseTilSamlingUbestemtDto::class, name = "UBESTEMT"),
)
sealed interface FaktaReiseTilSamlingDto {
    val type: TypeReiseTilSamling

    fun mapTilFakta(
        reiseId: ReiseId,
        adresse: String?,
    ): FaktaReiseTilSamling
}

data class FaktaReiseTilSamlingOffentligTransportDto(
    val utgifterOffentligTransport: Int?,
) : FaktaReiseTilSamlingDto {
    override val type = TypeReiseTilSamling.OFFENTLIG_TRANSPORT

    override fun mapTilFakta(
        reiseId: ReiseId,
        adresse: String?,
    ) = FaktaOffentligTransport(
        reiseId = reiseId,
        adresse = adresse,
        utgifterOffentligTransport = utgifterOffentligTransport,
    )
}

data class FaktaReiseTilSamlingPrivatBilDto(
    val reiseavstand: BigDecimal?,
) : FaktaReiseTilSamlingDto {
    override val type = TypeReiseTilSamling.PRIVAT_BIL

    override fun mapTilFakta(
        reiseId: ReiseId,
        adresse: String?,
    ) = FaktaPrivatBil(
        reiseId = reiseId,
        adresse = adresse,
        reiseavstand = reiseavstand,
    )
}

data object FaktaReiseTilSamlingUbestemtDto : FaktaReiseTilSamlingDto {
    override val type = TypeReiseTilSamling.UBESTEMT

    override fun mapTilFakta(
        reiseId: ReiseId,
        adresse: String?,
    ) = FaktaUbestemtType(
        reiseId = reiseId,
        adresse = adresse,
    )
}
