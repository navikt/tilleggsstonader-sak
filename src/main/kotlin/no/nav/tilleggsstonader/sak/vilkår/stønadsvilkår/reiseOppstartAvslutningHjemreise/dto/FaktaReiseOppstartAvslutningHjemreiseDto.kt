package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.NødvendigUtgiftDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaUbestemtType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.TypeReiseOppstartAvslutningHjemreise
import java.math.BigDecimal

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true,
)
@JsonSubTypes(
    JsonSubTypes.Type(FaktaReiseOppstartAvslutningHjemreiseOffentligTransportDto::class, name = "OFFENTLIG_TRANSPORT"),
    JsonSubTypes.Type(FaktaReiseOppstartAvslutningHjemreisePrivatBilDto::class, name = "PRIVAT_BIL"),
    JsonSubTypes.Type(FaktaReiseOppstartAvslutningHjemreiseUbestemtDto::class, name = "UBESTEMT"),
)
sealed interface FaktaReiseOppstartAvslutningHjemreiseDto {
    val type: TypeReiseOppstartAvslutningHjemreise

    fun mapTilFakta(): FaktaReiseOppstartAvslutningHjemreise
}

data class FaktaReiseOppstartAvslutningHjemreiseOffentligTransportDto(
    val nødvendigUtgift: NødvendigUtgiftDto,
    val utgifterOffentligTransport: BigDecimal,
    val begrunnelse: String? = null,
) : FaktaReiseOppstartAvslutningHjemreiseDto {
    override val type = TypeReiseOppstartAvslutningHjemreise.OFFENTLIG_TRANSPORT

    override fun mapTilFakta() =
        FaktaOffentligTransport(
            nødvendigUtgift = nødvendigUtgift.tilDomene(),
            utgifterOffentligTransport = utgifterOffentligTransport,
            begrunnelse = begrunnelse,
        )
}

data class FaktaReiseOppstartAvslutningHjemreisePrivatBilDto(
    val nødvendigUtgift: NødvendigUtgiftDto,
    val reiseavstand: BigDecimal,
    val bompenger: BigDecimal? = null,
    val fergekostnad: BigDecimal? = null,
    val parkering: BigDecimal? = null,
    val piggdekkavgift: BigDecimal? = null,
    val begrunnelse: String? = null,
) : FaktaReiseOppstartAvslutningHjemreiseDto {
    override val type = TypeReiseOppstartAvslutningHjemreise.PRIVAT_BIL

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

data object FaktaReiseOppstartAvslutningHjemreiseUbestemtDto : FaktaReiseOppstartAvslutningHjemreiseDto {
    override val type = TypeReiseOppstartAvslutningHjemreise.UBESTEMT

    override fun mapTilFakta() = FaktaUbestemtType
}
