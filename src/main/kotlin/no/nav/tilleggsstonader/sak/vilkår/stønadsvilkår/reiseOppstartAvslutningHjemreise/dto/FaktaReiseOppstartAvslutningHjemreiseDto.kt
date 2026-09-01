package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaUbestemtType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.TypeReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.TypeReiseformål
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
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

    fun mapTilFakta(
        reiseId: ReiseId,
        adresse: String?,
        typeReiseformål: TypeReiseformål,
    ): FaktaReiseOppstartAvslutningHjemreise
}

data class FaktaReiseOppstartAvslutningHjemreiseOffentligTransportDto(
    val utgifterOffentligTransport: BigDecimal,
    val aktivitetId: VilkårperiodeGlobalId,
) : FaktaReiseOppstartAvslutningHjemreiseDto {
    override val type = TypeReiseOppstartAvslutningHjemreise.OFFENTLIG_TRANSPORT

    override fun mapTilFakta(
        reiseId: ReiseId,
        adresse: String?,
        typeReiseformål: TypeReiseformål,
    ) = FaktaOffentligTransport(
        reiseId = reiseId,
        adresse = adresse,
        typeReiseformål = typeReiseformål,
        utgifterOffentligTransport = utgifterOffentligTransport,
        aktivitetId = aktivitetId,
    )
}

data class FaktaReiseOppstartAvslutningHjemreisePrivatBilDto(
    val reiseavstand: BigDecimal,
    val aktivitetId: VilkårperiodeGlobalId,
) : FaktaReiseOppstartAvslutningHjemreiseDto {
    override val type = TypeReiseOppstartAvslutningHjemreise.PRIVAT_BIL

    override fun mapTilFakta(
        reiseId: ReiseId,
        adresse: String?,
        typeReiseformål: TypeReiseformål,
    ) = FaktaPrivatBil(
        reiseId = reiseId,
        adresse = adresse,
        typeReiseformål = typeReiseformål,
        reiseavstand = reiseavstand,
        aktivitetId = aktivitetId,
    )
}

data object FaktaReiseOppstartAvslutningHjemreiseUbestemtDto : FaktaReiseOppstartAvslutningHjemreiseDto {
    override val type = TypeReiseOppstartAvslutningHjemreise.UBESTEMT

    override fun mapTilFakta(
        reiseId: ReiseId,
        adresse: String?,
        typeReiseformål: TypeReiseformål,
    ) = FaktaUbestemtType(
        reiseId = reiseId,
        adresse = adresse,
        typeReiseformål = typeReiseformål,
    )
}
