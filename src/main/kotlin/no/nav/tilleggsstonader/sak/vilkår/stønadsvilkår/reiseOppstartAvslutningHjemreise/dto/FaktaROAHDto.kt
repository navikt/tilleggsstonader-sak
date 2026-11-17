package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.NødvendigUtgiftDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaROAH
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaUbestemtType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.TypeFaktaROAH
import java.math.BigDecimal

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true,
)
@JsonSubTypes(
    JsonSubTypes.Type(FaktaROAHOffentligTransportDto::class, name = "OFFENTLIG_TRANSPORT"),
    JsonSubTypes.Type(FaktaROAHPrivatBilDto::class, name = "PRIVAT_BIL"),
    JsonSubTypes.Type(FaktaROAHUbestemtDto::class, name = "UBESTEMT"),
)
sealed interface FaktaROAHDto {
    val type: TypeFaktaROAH

    fun mapTilFakta(): FaktaROAH
}

data class FaktaROAHOffentligTransportDto(
    val nødvendigUtgift: NødvendigUtgiftDto,
    val utgifterOffentligTransport: BigDecimal,
    val begrunnelse: String? = null,
) : FaktaROAHDto {
    override val type = TypeFaktaROAH.OFFENTLIG_TRANSPORT

    override fun mapTilFakta() =
        FaktaOffentligTransport(
            nødvendigUtgift = nødvendigUtgift.tilDomene(),
            utgifterOffentligTransport = utgifterOffentligTransport,
            begrunnelse = begrunnelse,
        )
}

data class FaktaROAHPrivatBilDto(
    val nødvendigUtgift: NødvendigUtgiftDto,
    val reiseavstand: BigDecimal,
    val bompenger: BigDecimal? = null,
    val fergekostnad: BigDecimal? = null,
    val parkering: BigDecimal? = null,
    val piggdekkavgift: BigDecimal? = null,
    val begrunnelse: String? = null,
) : FaktaROAHDto {
    override val type = TypeFaktaROAH.PRIVAT_BIL

    override fun mapTilFakta() =
        FaktaPrivatBil(
            nødvendigUtgift = nødvendigUtgift.tilDomene(),
            reiseavstand = reiseavstand,
            bompenger = bompenger,
            fergekostnad = fergekostnad,
            parkering = parkering,
            piggdekkavgift = piggdekkavgift,
            begrunnelse = begrunnelse,
        )
}

data object FaktaROAHUbestemtDto : FaktaROAHDto {
    override val type = TypeFaktaROAH.UBESTEMT

    override fun mapTilFakta() = FaktaUbestemtType
}
